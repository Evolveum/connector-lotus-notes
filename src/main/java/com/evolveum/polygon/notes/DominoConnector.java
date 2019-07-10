/*
 * Copyright (c) 2014 Evolveum
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.evolveum.polygon.notes;

import com.evolveum.polygon.notes.util.DominoUtils;
import com.evolveum.polygon.notes.util.RegistrationBuilder;
import lotus.domino.*;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.Validate;
import org.identityconnectors.common.logging.Log;
import org.identityconnectors.common.security.GuardedString;
import org.identityconnectors.framework.common.exceptions.AlreadyExistsException;
import org.identityconnectors.framework.common.exceptions.ConnectorException;
import org.identityconnectors.framework.common.exceptions.UnknownUidException;
import org.identityconnectors.framework.common.objects.*;
import org.identityconnectors.framework.common.objects.Name;
import org.identityconnectors.framework.common.objects.filter.ContainsFilter;
import org.identityconnectors.framework.common.objects.filter.EqualsFilter;
import org.identityconnectors.framework.common.objects.filter.Filter;
import org.identityconnectors.framework.common.objects.filter.FilterTranslator;
import org.identityconnectors.framework.spi.Configuration;
import org.identityconnectors.framework.spi.ConnectorClass;
import org.identityconnectors.framework.spi.PoolableConnector;
import org.identityconnectors.framework.spi.operations.*;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static com.evolveum.polygon.notes.DominoAccountAttribute.*;
import static com.evolveum.polygon.notes.DominoGroupAttribute.*;
import static com.evolveum.polygon.notes.DominoOperationOption.*;
import static com.evolveum.polygon.notes.util.DominoUtils.*;

/**
 * Documentation:
 * http://publib.boulder.ibm.com/infocenter/domhelp/v8r0/index.jsp?topic=%2Fcom.ibm.designer.domino.main.doc%2FH_JAVA_NOTES_CLASSES_JAVA.html
 */
@ConnectorClass(displayNameKey = "UI_CONNECTOR_NAME",
        configurationClass = DominoConfiguration.class)
public class DominoConnector implements PoolableConnector, CreateOp, SchemaOp, TestOp, DeleteOp,
        UpdateAttributeValuesOp, SearchOp<String> {

    private static final Log LOG = Log.getLog(DominoConnector.class);

    private static enum Update {
        ADD, REMOVE, REPLACE
    }

    private static final String PREFIX_ROAMING = "roaming/";
    private static final String PREFIX_MAIL = "mail\\";
    private static final String RENAME_NO_CHANGE = "*";
    private static final String NEW_HOME_SERVER_MAIL_PATH = "mail";

    private static final Pattern GET_QUERY;

    static {
        GET_QUERY = Pattern.compile("\\(NoteID=\"([a-zA-Z0-9]+)\"\\)");
    }

    private DominoConfiguration config;
    private DominoConnection connection;

    public DominoConfiguration getConfiguration() {
        return config;
    }

    public void checkAlive() {
        LOG.info("checkAlive::start");
        if (connection != null) {
            LOG.ok("Checking if domino connection is alive.");
            connection.checkAlive();
        } else {
            LOG.ok("Creating new domino connection.");
            connection = new DominoConnection(this.config);
        }
        LOG.info("checkAlive::finish");
    }

    public void init(Configuration config) {
        LOG.info("init::start");

        Validate.notNull(config, "Configuration must not be null.");
        Validate.isTrue(config instanceof DominoConfiguration, "Configuration must be instance of '"
                + DominoConfiguration.class.getName() + "', but it's instance of '"
                + config.getClass().getName() + "'.");

        this.config = (DominoConfiguration) config;
        this.connection = new DominoConnection(this.config);

        LOG.info("init::finish");
    }

    public void dispose() {
        LOG.info("dispose::start");
        if (connection != null) {
            connection.dispose();
        }
        LOG.info("dispose::finish");
    }

    public void test() {
        LOG.info("test::start");
        config.validate();
        connection.checkAlive();
        LOG.info("test::finish");
    }

    public Schema schema() {
        LOG.info("schema::start");
        SchemaBuilder schema = new SchemaBuilder(DominoConnector.class);
        //account
        Set<AttributeInfo> attributes = createAttributes(DominoAccountAttribute.class);
        schema.defineObjectClass(ObjectClass.ACCOUNT_NAME, attributes);
        //group
        attributes = createAttributes(DominoGroupAttribute.class);
        ObjectClassInfoBuilder objectClassInfo = new ObjectClassInfoBuilder();
        objectClassInfo.setType(ObjectClass.GROUP_NAME);
        objectClassInfo.addAllAttributeInfo(attributes);
        ObjectClassInfo group = objectClassInfo.build();
        schema.defineObjectClass(group);

        for (DominoOperationOption option : DominoOperationOption.values()) {
            schema.defineOperationOption(option.getInfo());
        }

        schema.clearSupportedOptionsByOperation();
        schema.addSupportedOperationOption(CreateOp.class, DominoOperationOption.SYNCH_INTERNET_PASSWORD.getInfo());
        schema.addSupportedOperationOption(CreateOp.class, DominoOperationOption.MAIL_OWNER_ACCESS.getInfo());
        schema.addSupportedOperationOption(DeleteOp.class, DominoOperationOption.MAIL_FILE_ACTION.getInfo());
        schema.addSupportedOperationOption(DeleteOp.class, DominoOperationOption.DELETE_WINDOWS_USER.getInfo());

        Schema retVal = schema.build();

        LOG.info("schema::finish");
        return retVal;
    }

    private Set<AttributeInfo> createAttributes(Class<? extends DominoAttribute> type) {
        Set<AttributeInfo> infos = new HashSet<AttributeInfo>();
        for (DominoAttribute attr : type.getEnumConstants()) {
            if (attr.getAttribute() != null) {
                infos.add(attr.getAttribute());
            } else {
                infos.add(AttributeInfoBuilder.build(attr.getName(), attr.getType(), attr.getFlags()));
            }
        }

        return infos;
    }

    public FilterTranslator<String> createFilterTranslator(ObjectClass oclass, OperationOptions options) {
        LOG.info("createFilterTranslator::start");
        LOG.ok("Parameters: oc: {0}, op: {1}", oclass, options);

        DominoFilterTranslator translator = new DominoFilterTranslator(connection, config, oclass);

        LOG.info("createFilterTranslator::finish");
        return translator;
    }

    public void executeQuery(ObjectClass oclass, String query, ResultsHandler handler, OperationOptions options) {
        LOG.info("executeQuery::start");
        LOG.ok("Parameters: oc: {0}, q: {1}, op: {2}", oclass, query, options);

        Validate.notNull(oclass, "Object class must not be null.");
        Validate.notNull(handler, "Results handler must not be null.");

        String realQuery = createRealQuery(oclass, query);
        try {
            Database userDatabase = connection.getUserDatabase();
            Matcher matcher = null;
            if (query != null) {
                matcher = GET_QUERY.matcher(query);
            }
            if (query != null && matcher.matches()) {
                try {
                    Document document = userDatabase.getDocumentByUNID(matcher.group(1));

                    Set<String> attributes = createAttributesToGet(oclass, options);
                    ConnectorObject object = createConnectorObject(document, oclass, attributes);
                    handler.handle(object);
                    LOG.info("Search returned 1 object (get document by UNID).");
                } catch (NotesException ex) {
                    if (NotesError.NOTES_ERR_BAD_UNID != ex.id) {
                        throw ex;
                    }
                }
            } else {
                DocumentCollection collection = userDatabase.search(realQuery);
                Document document = collection.getFirstDocument();

                int count = 0;
                Set<String> attributes = createAttributesToGet(oclass, options);
                while (document != null) {
                    count++;
                    ConnectorObject object = createConnectorObject(document, oclass, attributes);
                    if (!handler.handle(object)) {
                        break;
                    }
                    document = collection.getNextDocument();
                }
                LOG.info("Search returned {0} objects.", count);
            }
        } catch (NotesException ex) {
            handleException(ex, "Couldn't execute query", LOG);
        }

        LOG.info("executeQuery::finish");
    }

    private String createRealQuery(ObjectClass oclass, String query) {
        StringBuilder sb = new StringBuilder();

        String form;
        if (ObjectClass.ACCOUNT.equals(oclass)) {
            form = DominoConstants.FORM_PERSON;
        } else if (ObjectClass.GROUP.equals(oclass)) {
            form = DominoConstants.FORM_GROUP;
        } else {
            throw new ConnectorException("Unknown object class '" + oclass + "'.");
        }

        sb.append("(form='").append(form).append("')");

        if (StringUtils.isNotEmpty(query)) {
            sb.append("&").append(query);
        }

        return sb.toString();
    }

    /**
     * @param document  represents real object on Domino (target system)
     * @param oclass    connector object type (account, group, etc.)
     * @param attrToGet attributes to be returned in {@link org.identityconnectors.framework.common.objects.ConnectorObject}
     * @return
     * @throws lotus.domino.NotesException
     */
    private ConnectorObject createConnectorObject(Document document, ObjectClass oclass, Set<String> attrToGet)
            throws NotesException {
        if (document == null) {
            return null;
        }

        ConnectorObjectBuilder object = new ConnectorObjectBuilder();
        String fullNameValue = null;
        for (Item item : (Vector<Item>) document.getItems()) {
            String name = item.getName();
            if (!attrToGet.contains(name) && !isNameAttribute(oclass, name)) {
                // unknown attribute or API is not asking for this attribute
                continue;
            }

            List<Object> values = createAttributeValues(item);
            if (isNameAttribute(oclass, name)) {
                String objectName = fullNameValue = getFirstValueString(values);
                if (ObjectClass.ACCOUNT.equals(oclass)) {
                    if (objectName != null) {
                        objectName = getAbbreviated(connection, objectName);

                        Attribute attr = build(CERTIFIER_ORG_HIERARCHY, getOrgFromName(connection, objectName));
                        object.addAttribute(attr);

                        attr = build(ORG_UNIT, getOrgUnit(connection, objectName));
                        object.addAttribute(attr);
                    } else {
                        objectName = document.getItemValueString(LAST_NAME.getName());
                    }
                } else if (ObjectClass.GROUP.equals(oclass) && objectName != null) {
                    object.addAttribute(build(DISPLAY_NAME, objectName));

                    objectName = getGroupFullName(item.getValues());
                    values.clear();
                    values.add(objectName);

                    object.setUid(objectName);
                }
                object.setName(objectName);
            }
            if (values.isEmpty()) {
                continue;
            }

            //handle groups and members
            if (ObjectClass.ACCOUNT.equals(oclass)) {
                if (MAIL_FILE.getName().equals(name) && (isAttrToGet(attrToGet, MAIL_QUOTA_SIZE_LIMIT)
                        || isAttrToGet(attrToGet, MAIL_QUOTA_WARNING_THRESHOLD))) {
                    String mailDbName = values.size() > 0 ? item.getValueString() : null;
                    addMailQuotaAttributes(object, mailDbName, attrToGet);
                }
            } else if (ObjectClass.GROUP.equals(oclass)) {
                if (MEMBERS.getName().equals(name) && (isAttrToGet(attrToGet, MEMBER_GROUPS)
                        || isAttrToGet(attrToGet, MEMBER_PEOPLE))) {
                    //handle group members
                    addGroupMemberPeople(object, values, attrToGet);
                }
            }

            if (isAttrToGet(attrToGet, name)) {
                //simply add attribute to connector object
                DominoAttribute accAttr;

                if (ObjectClass.ACCOUNT.equals(oclass)) {
                    accAttr = DominoAccountAttribute.getAttribute(name);
                } else if (ObjectClass.GROUP.equals(oclass)) {
                    accAttr = DominoGroupAttribute.getAttribute(name);
                } else {
                    accAttr = null;
                }

                if ((accAttr != null) && (GuardedString.class.equals(accAttr.getType()))) {
                    String guarded = (String) values.get(0);
                    object.addAttribute(AttributeBuilder.build(name, new GuardedString(guarded.toCharArray())));
                } else {
                    object.addAttribute(AttributeBuilder.build(name, values));
                }
            }
        }

        String uid = getGuid(document.getUniversalID());
        if (ObjectClass.ACCOUNT.equals(oclass)) {
            object.setUid(uid);

            Item chkItem = document.getFirstItem(CHECK_PASSWORD.getName());
            boolean enabled = chkItem == null
                    || !Integer.toString(AdministrationProcess.PWD_CHK_LOCKOUT).equals(chkItem.getText());
            object.addAttribute(AttributeBuilder.buildEnabled(enabled));

            if (isAttrToGet(attrToGet, GROUP_LIST) && fullNameValue != null) {
                addUsersToGroupList(object, fullNameValue);
            }
        } else if (ObjectClass.GROUP.equals(oclass)) {
            object.addAttribute(build(DominoGroupAttribute.OBJECT_GUID, uid));
        }

        if (document.getLastModified() != null) {
            DominoAttribute attr = ObjectClass.ACCOUNT.equals(oclass) ? DominoAccountAttribute.LAST_MODIFIED :
                    DominoGroupAttribute.LAST_MODIFIED;
            object.addAttribute(AttributeBuilder.build(attr.getName(),
                    Long.valueOf(document.getLastModified().toJavaDate().getTime())));
        }

        return object.build();
    }

    private void addUsersToGroupList(ConnectorObjectBuilder object, String fullName) throws NotesException {
        List<String> groups = getGroupList(fullName);

        AttributeBuilder attr = new AttributeBuilder();
        attr.setName(GROUP_LIST.getName());
        for (String group : groups) {
            String displayName = getGroupDisplayName(group);
            if (isDenyGroup(displayName)) {
                object.addAttribute(new Attribute[]{AttributeBuilder.buildEnabled(false)});
            } else {
                attr.addValue(displayName);
            }
        }

        if (attr.getValue() == null || attr.getValue().isEmpty()) {
            return;
        }

        object.addAttribute(attr.build());
    }

    private boolean isDenyGroup(String displayName) throws NotesException {
        DocumentCollection collection = null;
        try {
            EqualsFilter filter = new EqualsFilter(build(LIST_NAME, displayName));
            collection = getDocumentCollection(DominoConstants.FORM_GROUP, filter);
            Document group = collection.getFirstDocument();
            if (group != null) {
                String value = group.getItemValueString(GROUP_TYPE.getName());
                recycleQuietly(group);

                if ("3".equals(value)) {
                    return true;
                }
            }
        } finally {
            recycleQuietly(collection);
        }

        return false;
    }

    private List<String> getGroupList(String fullName) throws NotesException {
        List<String> groups = new ArrayList<String>();

        ContainsFilter filter = new ContainsFilter(build(MEMBERS, fullName));
        DocumentCollection collection = null;
        try {
            collection = getDocumentCollection(DominoConstants.FORM_GROUP, filter);
            Document document = collection.getFirstDocument();
            while (document != null) {
                groups.add(document.getItemValueString(LIST_NAME.getName()));
                recycleQuietly(document);
                document = collection.getNextDocument();
            }
        } finally {
            recycleQuietly(collection);
        }

        return groups;
    }

    private String getGroupFullName(List<String> values) {
        if (values == null) {
            return null;
        }

        return StringUtils.join(values, ";");
    }

    private void addMailQuotaAttributes(ConnectorObjectBuilder object, String mailDbname, Set<String> attrToGet)
            throws NotesException {
        LOG.info("Adding mail quota attributes for mail db {0}.", mailDbname);

        Session session = connection.getSession();
        Database db = null;
        try {
            db = session.getDatabase(null, mailDbname, false);
            if (db == null) {
                return;
            }

            if (isAttrToGet(attrToGet, MAIL_QUOTA_SIZE_LIMIT)) {
                object.addAttribute(build(MAIL_QUOTA_SIZE_LIMIT, Integer.valueOf(db.getSizeQuota())));
            }
            if (isAttrToGet(attrToGet, MAIL_QUOTA_WARNING_THRESHOLD)) {
                Number size = db.getSizeWarning();
                object.addAttribute(build(MAIL_QUOTA_WARNING_THRESHOLD, size.intValue()));
            }
        } catch (NotesException ex) {
            if (ex.id == NotesError.NOTES_ERR_DBNOACCESS) {
                LOG.error("User {0} doesn't have rights to access DB quota limits for {1}, reason: {2}",
                        config.getAdminName(), mailDbname, getExceptionMessage(ex));
                return;
            }
            LOG.error(ex, "Couldn't get mail quota attributes for user, reason: {0}", getExceptionMessage(ex));
            throw ex;
        } finally {
            recycleQuietly(db);
        }
    }

    private void addGroupMemberPeople(ConnectorObjectBuilder object, List<Object> values, Set<String> attrToGet)
            throws NotesException {
        List<String> groups = new ArrayList<String>();
        List<String> peoples = new ArrayList<String>();

        for (Object value : values) {
            String fullName = getCanonical(connection, value.toString());

            if (isAttrToGet(attrToGet, MEMBER_GROUPS) && checkIfGroupExists(fullName)) {
                groups.add(fullName);
            }

            if (isAttrToGet(attrToGet, MEMBER_PEOPLE) && checkIfUserExist(fullName)) {
                peoples.add(fullName);
            }
        }

        if (isAttrToGet(attrToGet, MEMBER_GROUPS)) {
            object.addAttribute(build(MEMBER_GROUPS, groups.toArray()));
        }

        if (isAttrToGet(attrToGet, MEMBER_PEOPLE)) {
            object.addAttribute(build(MEMBER_PEOPLE, peoples.toArray()));
        }
    }

    private List<Object> createAttributeValues(Item item) throws NotesException {
        List<Object> values = new ArrayList<Object>();

        String name = item.getName();
        for (Object value : item.getValues()) {
            if (PASSWORD_CHANGE_INTERVAL.getName().equals(name)
                    || PASSWORD_GRACE_PERIOD.getName().equals(name)
                    || ROAM_CLEAN_PER.getName().equals(name)) {

                values.add(((Double) value).intValue());
            } else if (ROAM_CLEAN_SETTING.getName().equals(name)) {
                values.add(Integer.valueOf((String) value));
            } else if (MAIL_SERVER.getName().equals(name)
                    || MEMBERS.getName().equals(name)) {
                String abbreviated = DominoUtils.getAbbreviated(connection, (String) value);
                values.add(abbreviated);
            } else if (value instanceof DateTime) {
                //translating to long (time in millis)
                DateTime dateTime = (DateTime) value;
                values.add(dateTime.toJavaDate().getTime());
            } else if (value instanceof Number) {
                values.add(value);
            } else if (value != null) {
                values.add(value.toString());
            } else {
                values.add(null);
            }
        }

        return values;
    }

    private static boolean isNameAttribute(ObjectClass oclass, String name) {
        if (oclass == null || name == null) {
            return false;
        }

        if (ObjectClass.ACCOUNT.equals(oclass) && DominoAccountAttribute.FULL_NAME.getName().equals(name)) {
            return true;
        }

        if (ObjectClass.GROUP.equals(oclass) && DominoGroupAttribute.LIST_NAME.getName().equals(name)) {
            return true;
        }

        return false;
    }

    public Uid create(ObjectClass oclass, Set<Attribute> attrs, OperationOptions options) {
        LOG.info("create::start");
        LOG.ok("Parameters: oc: {0}, a: {1}, op: {2}", oclass, attrs, options);

        Validate.notNull(oclass, "Object class must not be null.");
        Validate.notNull(attrs, "Attributes must not be null.");

        Uid uid;
        try {
            Map<String, Attribute> attributes = new HashMap<String, Attribute>(AttributeUtil.toMap(attrs));
            if (ObjectClass.ACCOUNT.equals(oclass)) {
                uid = createAccount(attributes, options);
            } else if (ObjectClass.GROUP.equals(oclass)) {
                uid = createGroup(attributes, options);
            } else {
                LOG.ok("Unknown object class '{0}'.", oclass);
                throw new IllegalArgumentException("Unknown object class '" + oclass + "'.");
            }
        } catch (NotesException ex) {
            handleException(ex, "Couldn't create " + oclass.getObjectClassValue(), LOG);
            return null;
        }

        LOG.info("create::finish");
        return uid;
    }

    private Uid createAccount(Map<String, Attribute> attrs, OperationOptions options) throws NotesException {
        LOG.ok("Creating account.");

        Name fullNameAttr = (Name) attrs.remove(Name.NAME);
        String fullNameNormalized = normalizeSpaces(fullNameAttr.getNameValue());
        String fullName = getCanonical(connection, fullNameNormalized);
        LOG.ok("Full name {0}, checking if user exists.", fullName);
        if (checkIfUserExist(fullName)) {
            throw new AlreadyExistsException("User '" + fullName + "' already exists.");
        }

        String firstName = getAttributeValue(attrs, FIRST_NAME);
        String middleInitial = getAttributeValue(attrs, MIDDLE_INITIAL);
        String lastName = getAttributeValue(attrs, LAST_NAME);

        String idFile = getAttributeValue(attrs, ID_FILE);

        String mailFile = getAttributeValue(attrs, MAIL_FILE);
        if (StringUtils.isEmpty(mailFile)) {
            StringBuilder sb = new StringBuilder();
            sb.append(PREFIX_MAIL);
            sb.append(StringUtils.isNotEmpty(firstName) ? firstName.toLowerCase() : "");
            sb.append(StringUtils.isNotEmpty(lastName) ? lastName.toLowerCase() : "");
            mailFile = sb.toString();
        }
        String mailServer = getAttributeValue(attrs, MAIL_SERVER, String.class, config.getMailServer());
        String forwardingAddress = getAttributeValue(attrs, FORWARDING_ADDRESS);

        String location = getAttributeValue(attrs, LOCATION);
        String comment = getAttributeValue(attrs, COMMENT);

        String certPw = decode(getAttributeValue(attrs, CREDENTIALS, GuardedString.class));
        if (StringUtils.isEmpty(certPw)) {
            certPw = decode(config.getCertifierPassword());
        }
        String userPw = decode(getAttributeValue(attrs, PASSWORD, GuardedString.class));

        String orgUnit = getAttributeValue(attrs, ORG_UNIT);
        String mailTemplate = getAttributeValue(attrs, MAIL_TEMPLATE_NAME, String.class, config.getMailTemplateName());
        Integer mailQuotaSize = getAttributeValue(attrs, MAIL_QUOTA_SIZE_LIMIT, Integer.class);
        Integer mailQuotaWThreshold = getAttributeValue(attrs, MAIL_QUOTA_WARNING_THRESHOLD, Integer.class);

        //roaming
        String roamRplSrvrs = getAttributeValue(attrs, ROAM_RPL_SRVRS);
        if (StringUtils.isEmpty(roamRplSrvrs) && StringUtils.isNotEmpty(config.getRoamRplSrvrs())) {
            Attribute roamRplSrvrsAttr = build(ROAM_RPL_SRVRS, config.getRoamRplSrvrs());
            attrs.put(ROAM_RPL_SRVRS.getName(), roamRplSrvrsAttr);
        }

        String certifierOrgHierarchy = getAttributeValue(attrs, CERTIFIER_ORG_HIERARCHY);

        String altNameLang = getAttributeValue(attrs, ALT_FULL_NAME_LANGUAGE);
        String altName = getAttributeValue(attrs, ALT_FULL_NAME);
        if (StringUtils.isNotEmpty(altName)) {
            altName = getCanonical(connection, altName);
        }

        Registration registration = null;
        try {
            LOG.ok("Creating registration.");

            registration = createAndSetupRegistration(attrs, options, firstName, lastName, orgUnit, mailTemplate,
                    mailQuotaSize, mailQuotaWThreshold, certPw);

            fullName = createFullName(connection, firstName, middleInitial, lastName, orgUnit, certifierOrgHierarchy);
            boolean added;
            if (config.getCreateMailDbInBackground() != null && config.getCreateMailDbInBackground()) {
                registration.setCreateMailDb(false);
                added = registration.registerNewUser(lastName, idFile, mailServer, firstName, middleInitial,
                        certPw, location, comment, mailFile, forwardingAddress, userPw, altName, altNameLang);

                createMailDbInBackground(fullName, mailServer, mailFile, mailTemplate, certifierOrgHierarchy,
                        mailQuotaSize, mailQuotaWThreshold);
            } else {
                added = registration.registerNewUser(lastName, idFile, mailServer, firstName, middleInitial,
                        certPw, location, comment, mailFile, forwardingAddress, userPw, altName, altNameLang);
            }

            if (!added) {
                throw new ConnectorException("Couldn't create user '" + fullName + "'.");
            }

            return updateAccount(new Name(fullName), attrs);
        } finally {
            recycleQuietly(registration);
        }
    }

    private Registration createAndSetupRegistration(Map<String, Attribute> attrs, OperationOptions options,
                                                    String firstName, String lastName, String orgUnit,
                                                    String mailTemplate, Integer mailQuotaSize,
                                                    Integer mailQuotaWThreshold, String certPw) throws NotesException {

        Vector<String> shortNameVector = getAttributeValue(attrs, SHORT_NAME, Vector.class, null, false);
        String shortName = null;
        if (shortNameVector != null && !shortNameVector.isEmpty()) {
            shortName = shortNameVector.get(0);
        }

        String policy = getAttributeValue(attrs, POLICY, String.class, config.getPolicy());

        Integer defaultPasswordExp = getAttributeValue(attrs, DEFAULT_PASSWORD_EXP, Integer.class);
        Integer days = getDays(getAttributeValue(attrs, END_DATE, Long.class));
        if (defaultPasswordExp == null) {
            if (days != null && days > 0L) {
                defaultPasswordExp = days;
            } else {
                defaultPasswordExp = config.getDefaultPasswordExp();
            }
        }
        Vector altOrgUnit = getAttributeValue(attrs, ALT_ORG_UNIT, Vector.class);

        Vector mailReplicaServers = getAttributeValue(attrs, ROAM_RPL_SRVRS, Vector.class);
        Boolean northAmerican = getAttributeValue(attrs, NORTH_AMERICAN, Boolean.class, config.getNorthAmerican());

        String certifierIdFile = getAttributeValue(attrs, CERTIFIER_ID_FILE, String.class, config.getCertifierIdFile());
        String caCertifier = getAttributeValue(attrs, CA_CERTIFIER, String.class, config.getCaCertifierName());
        if (config.getUseCAProcess() != null && config.getUseCAProcess()) {
            if (StringUtils.isEmpty(caCertifier)) {
                throw new ConnectorException("Using CA process, but CA certifier name not defined (caCertifier attribute).");
            }
        } else {
            if (StringUtils.isEmpty(certifierIdFile)) {
                throw new ConnectorException("Certifier ID file not defined.");
            }
            if (StringUtils.isEmpty(certPw)) {
                throw new ConnectorException("Cert. password (credentials) attribute not defined.");
            }
        }

        //roaming
        String roamSrvr = getAttributeValue(attrs, ROAM_SRVR, String.class, config.getRoamSrvr());
        Integer roamCleanSetting = getAttributeValue(attrs, ROAM_CLEAN_SETTING, Integer.class, config.getRoamCleanSetting());
        Integer roamCleanPer = getAttributeValue(attrs, ROAM_CLEAN_PER, Integer.class, config.getRoamCleanPer());
        String roamSubDir = getAttributeValue(attrs, ROAM_SUBDIR, String.class, PREFIX_ROAMING + firstName + lastName);

        //groups validation
        Vector groups = getAttributeValue(attrs, GROUP_LIST, Vector.class);
        checkIfGroupsExist(groups);

        String internetAddress = getAttributeValue(attrs, INTERNET_ADDRESS);
        Boolean synchInternetPassword = getOperationOptionValue(options, SYNCH_INTERNET_PASSWORD, null);
        Integer mailOwnerAccess = getOperationOptionValue(options, MAIL_OWNER_ACCESS, config.getMailOwnerAccess());

        LOG.ok("Creating registration.");
        Registration registration = connection.getSession().createRegistration();
        RegistrationBuilder builder = new RegistrationBuilder(registration);
        builder.setCertifierName(caCertifier);
        builder.setPolicyName(policy);
        builder.setShortName(shortName);
        builder.setOrgUnit(orgUnit);
        builder.setAltOrgUnit(altOrgUnit);
        builder.setCertifierIDFile(certifierIdFile);
        builder.setMailTemplateName(mailTemplate);
        builder.setMailQuotaSizeLimit(mailQuotaSize);
        builder.setMailQuotaWarningThreshold(mailQuotaWThreshold);
        builder.setGroupList(groups);
        builder.setRoamingServer(roamSrvr);
        builder.setRoamingCleanupSetting(roamCleanSetting);
        builder.setRoamingCleanupPeriod(roamCleanPer);
        builder.setRoamingSubdir(roamSubDir);
        builder.setMailInternetAddress(internetAddress);
        builder.setSynchInternetPassword(synchInternetPassword);
        builder.setMailOwnerAccess(mailOwnerAccess);
        builder.setMailReplicaServers(mailReplicaServers);
        builder.setNorthAmerican(northAmerican);
        builder.setRegistrationLog(config.getRegistrationLog());
        builder.setStoreIDInAddressBook(config.getStoreIDInAddressBook());
        builder.setStoreIDInMailfile(config.getStoreIDInMailfile());
        builder.setIDType(config.getRealIdType());
        builder.setMailSystem(config.getMailSystem());
        builder.setCreateMailDb(config.getCreateMailDb());
        builder.setMinPasswordLength(config.getMinPasswordLength());
        builder.setUpdateAddressBook(true);

        if (StringUtils.isNotEmpty(config.getMailACLManager())) {
            builder.setMailACLManager(config.getMailACLManager());
        }

        builder.setMailACLManager("LocalDomainAdmins");

        if (config.getCreateIdFile() != null && !config.getCreateIdFile()) {
            builder.setNoIDFile(true);
            builder.setCertifierName(config.getAdminName());
        }

        DateTime expiration = connection.getSession().createDateTime("Today");
        expiration.setNow();
        expiration.adjustDay(defaultPasswordExp);
        builder.setExpiration(expiration);

        return registration;
    }

    private void checkIfGroupsExist(Vector<String> groups) throws NotesException {
        if (groups == null) {
            return;
        }

        for (String group : groups) {
            if (!checkIfGroupExists(group)) {
                throw new ConnectorException("Group '" + group + "' doesn't exists.");
            }
        }
    }

    private Document getUserByName(String name) throws NotesException {
        EqualsFilter filter = new EqualsFilter(new Name(name));
        DocumentCollection collection = getDocumentCollection(DominoConstants.FORM_PERSON, filter);

        return collection != null ? collection.getFirstDocument() : null;
    }

    private Document getUserByUid(String uid) throws NotesException {
        return connection.getUserDatabase().getDocumentByUNID(uid);
    }

    private Document getGroup(String listName) throws NotesException {
        EqualsFilter filter = new EqualsFilter(build(LIST_NAME, listName));
        DocumentCollection collection = getDocumentCollection(DominoConstants.FORM_GROUP, filter);

        return collection != null ? collection.getFirstDocument() : null;
    }

    private boolean checkIfGroupExists(String listName) throws NotesException {
        return getGroup(listName) != null;
    }

    private boolean checkIfUserExist(String fullName) throws NotesException {
        DocumentCollection collection = null;
        try {
            collection = getDocumentCollection(DominoConstants.FORM_PERSON, new EqualsFilter(new Name(fullName)));
            return collection != null && collection.getFirstDocument() != null;
        } finally {
            recycleQuietly(collection);
        }
    }

    private DocumentCollection getDocumentCollection(String form, Filter filter) throws NotesException {
        ObjectClass oclass = DominoConstants.FORM_PERSON.equals(form) ? ObjectClass.ACCOUNT : ObjectClass.GROUP;
        DominoFilterTranslator translator = new DominoFilterTranslator(connection, config, oclass);
        List<String> queries = translator.translate(filter);
        String query = queries.size() > 0 ? queries.get(0) : null;

        StringBuilder sb = new StringBuilder();
        // returns value of specified field. @GetField("form")="Person" - "form" field value must be
        // equal to "Person" if we're looking for users.
        sb.append("(@GetField(\"form\") = \"").append(form).append("\")");
        if (StringUtils.isNotEmpty(query)) {
            sb.append("&").append(query);
        }

        return connection.getUserDatabase().search(sb.toString());
    }

    private void createMailDbInBackground(String fullName, String mailServer, String mailFile, String mailTemplate,
                                          String certifierOrgHierarchy, Integer mailQuotaSize,
                                          Integer mailQuotaWThreshold) {
        LOG.ok("Creating mail db in background for '{0}'", fullName);

        try {
            String adminNameCanonical = getCanonical(connection, config.getAdminName());

            Session session = connection.getSession();
            Database adminP = session.getDatabase(config.getRegistrationServer(), "admin4.nsf", false);
            Document request = adminP.createDocument();
            request.appendItemValue("Form", "AdminRequest");
            request.appendItemValue("FullName", adminNameCanonical);
            request.appendItemValue("ProxyAction", "24");   //create mail file
            request.appendItemValue("ProxyAuthor", adminNameCanonical);
            request.appendItemValue("ProxyCreateFullTextIndex", "0");
            request.appendItemValue("ProxyDatabasePath", mailFile);
            request.appendItemValue("ProxyMailfileAccessLevel", "2");
            request.appendItemValue("ProxyNameList", fullName);
            request.appendItemValue("ProxyOriginatingAuthor", adminNameCanonical);

            String originatingOrg = certifierOrgHierarchy != null ? certifierOrgHierarchy.replace("/", "") : null;
            request.appendItemValue("ProxyOriginatingOrganization", originatingOrg);
            request.appendItemValue("ProxyOverrideDefaultDatastore", "0");
            request.appendItemValue("ProxyProcess", "Adminp");

            String mailServerCanonical = getCanonical(connection, mailServer);
            request.appendItemValue("ProxyServer", mailServerCanonical);
            request.appendItemValue("ProxySourceServer", mailServerCanonical);
            if (mailTemplate == null) {
                mailTemplate = "mail85.ntf";    //default mail template
            }
            request.appendItemValue("ProxyTextItem1", mailTemplate);
            if (mailQuotaSize != null) {
                request.appendItemValue("ProxyNumItem1", mailQuotaSize);
            }
            if (mailQuotaWThreshold != null) {
                request.appendItemValue("ProxyNumItem2", mailQuotaWThreshold);
            }
            request.appendItemValue("Type", "AdminRequest");

            Vector<Item> items = request.getItems();
            for (Item item : items) {
                item.setSigned(true);
            }

            request.sign();
            request.save();
        } catch (NotesException ex) {
            handleException(ex, "Couldn't create mail db in background for user '" + fullName + "'", LOG);
        }
    }

    private Uid createGroup(Map<String, Attribute> attributes, OperationOptions options) throws NotesException {
        Name listNameAttribute = (Name) attributes.remove(Name.NAME);

        Vector members = getAttributeValue(attributes, MEMBERS, Vector.class);
        Vector memberGroups = getAttributeValue(attributes, MEMBER_GROUPS, Vector.class);
        Vector memberPeople = getAttributeValue(attributes, MEMBER_PEOPLE, Vector.class);
        attributes.remove(DISPLAY_NAME.getName());

        if (listNameAttribute == null) {
            throw new ConnectorException("Missing attribute '" + Name.NAME + "'.");
        }

        String[] listNameArray = listNameAttribute.getNameValue().split(";");
        Vector listName = new Vector(Arrays.asList(listNameArray));
        String mainName = getGroupDisplayName(listNameAttribute.getNameValue());

        if (checkIfGroupExists(mainName)) {
            LOG.ok("Group '{0}' already exists.", mainName);
            throw new AlreadyExistsException("Group '" + mainName + "' already exists.");
        }

        Document group = connection.getUserDatabase().createDocument();
        for (Attribute attribute : attributes.values()) {
            group.replaceItemValue(attribute.getName(), getDominoValues(attribute));
        }

        List<String> membersList = getGroupMembers(members, memberGroups, memberPeople);
        if (membersList.size() > 0) {
            group.replaceItemValue(MEMBERS.getName(), membersList);
        }

        group.replaceItemValue(DominoConstants.FORM, DominoConstants.FORM_GROUP);
        group.replaceItemValue(DominoConstants.TYPE, DominoConstants.FORM_GROUP);
        group.replaceItemValue(LIST_NAME.getName(), listName);
        if (!group.save()) {
            LOG.ok("Can't save group '{0}'.", mainName);
            throw new ConnectorException("Can't save group '" + mainName + "'.");
        }

        group = getGroup(mainName);
        return createGroupUid(group);
    }

    private Uid createGroupUid(Document group) throws NotesException {
        String uid = getGroupFullName(group.getItemValue(LIST_NAME.getName()));
        return new Uid(uid);
    }

    private List<String> getGroupMembers(Vector members, Vector memberGroups, Vector memberPeople)
            throws NotesException {
        List list = new ArrayList();
        if (members == null && memberGroups != null) {
            list.addAll(memberGroups);
        }
        if (members == null && memberPeople != null) {
            list.addAll(memberPeople);
        }
        if (members != null) {
            list.addAll(members);
        }

        List<String> result = new ArrayList<String>();
        for (Object member : list) {
            result.add(getCanonical(connection, (String) member));
        }

        return result;
    }

    public Uid addAttributeValues(ObjectClass objclass, Uid uid, Set<Attribute> valuesToAdd, OperationOptions options) {
        return update(objclass, uid, valuesToAdd, options, Update.ADD);
    }

    public Uid removeAttributeValues(ObjectClass objclass, Uid uid, Set<Attribute> valuesToRemove, OperationOptions options) {
        return update(objclass, uid, valuesToRemove, options, Update.REMOVE);
    }

    public Uid update(ObjectClass objclass, Uid uid, Set<Attribute> replaceAttributes, OperationOptions options) {
        return update(objclass, uid, replaceAttributes, options, Update.REPLACE);
    }

    private Uid update(ObjectClass oclass, Uid uid, Set<Attribute> attrs, OperationOptions options, Update type) {
        LOG.info("update::start");
        LOG.ok("Parameters: oc: {0}, uid: {1}, t: {2}, a: {3}, op: {4}", oclass, uid, type, attrs, options);

        try {
            Map<String, Attribute> attributes = new HashMap<String, Attribute>(AttributeUtil.toMap(attrs));
            if (ObjectClass.ACCOUNT.equals(oclass)) {
                uid = updateAccount(uid, attributes, options, type);
            } else if (ObjectClass.GROUP.equals(oclass)) {
                uid = updateGroup(uid, attributes, options, type);
            } else {
                LOG.ok("Unknown object class '{0}'.", oclass);
                throw new IllegalArgumentException("Unknown object class '" + oclass + "'.");
            }
        } catch (NotesException ex) {
            handleException(ex, "Couldn't update " + oclass.getObjectClassValue() + ", uid: " + uid, LOG);
        }

        LOG.info("update::finish");
        return uid;
    }

    private Uid updateAccount(Name name, Map<String, Attribute> attrs) {
        Uid uid = null;
        Document document = null;
        try {
            document = getUserByName(name.getNameValue());
            if (document == null) {
                throw new ConnectorException("Document for account '" + name.getNameValue() + "' doesn't exist.");
            }
            uid = updateAccount(document, attrs, Update.REPLACE);
        } catch (NotesException ex) {
            handleException(ex, "Couldn't update user '" + name.getNameValue() + "'", LOG);
        } finally {
            recycleQuietly(document);
        }

        return uid;
    }

    private Uid updateAccount(Uid uid, Map<String, Attribute> attrs, OperationOptions options, Update type)
            throws NotesException {
        Document document = null;
        try {
            document = getUserByUid(getGuid(uid.getUidValue()));
            if (document == null) {
                throw new ConnectorException("Document with uid'" + uid.getUidValue() + "' doesn't exist.");
            }
            uid = updateAccount(document, attrs, type);
        } catch (NotesException ex) {
            handleException(ex, "Couldn't update user '" + getGuid(uid.getUidValue()) + "'", LOG);
        } finally {
            recycleQuietly(document);
        }

        return uid;
    }

    private Uid updateAccount(Document document, Map<String, Attribute> attrs, Update update) throws NotesException {
        Validate.notNull(document, "Document for update is null.");
        LOG.ok("updateAccount {0}", update);

        final Uid uid = new Uid(getGuid(document.getUniversalID()));
        if (attrs.isEmpty()) {
            return uid;
        }

        Vector fullNames = document.getItemValue(FULL_NAME.getName());
        String fullName = getCanonical(connection, fullNames.get(0).toString());
        String userHttpPw = decode(getAttributeValue(attrs, HTTP_PASSWORD, GuardedString.class));
        if (userHttpPw != null) {
            attrs.put(HTTP_PASSWORD.getName(), build(HTTP_PASSWORD, userHttpPw));
        }

        Integer pwdChangeInterval = getAttributeValue(attrs, PASSWORD_CHANGE_INTERVAL, Integer.class);
        Integer pwdGracePeriod = getAttributeValue(attrs, PASSWORD_GRACE_PERIOD, Integer.class);

        String currentPassword = decode(getAttributeValue(attrs, CURRENT_PASSWORD, GuardedString.class));
        String newPassword = decode(getAttributeValue(attrs, PASSWORD, GuardedString.class));

        List<String> denyGroups = getAttributeValue(attrs, DENY_GROUPS, Vector.class);
        if (denyGroups == null && config.getDisableDenyGroup() != null) {
            denyGroups = new Vector<String>();
            denyGroups.add(config.getDisableDenyGroup());
        }

        String idFile = getAttributeValue(attrs, ID_FILE);
        setPassword(fullName, currentPassword, newPassword, idFile);

        boolean changed = false;

        Integer roamCleanPer = getAttributeValue(attrs, ROAM_CLEAN_PER, Integer.class);
        if (roamCleanPer != null) {
            document.replaceItemValue(ROAM_CLEAN_PER.getName(), roamCleanPer);
            changed = true;
        }

        Integer roamCleanSetting = getAttributeValue(attrs, ROAM_CLEAN_SETTING, Integer.class);
        if (roamCleanSetting != null) {
            document.replaceItemValue(ROAM_CLEAN_SETTING.getName(), roamCleanSetting);
            changed = true;
        }

        Vector groups = getAttributeValue(attrs, GROUP_LIST, Vector.class);
        if (groups != null) {
            updateAccountGroupList(fullName, groups, update);
        }

        Boolean enable = getAttributeValue(attrs, ENABLE, Boolean.class);
        updateAccountSecurity(fullName, enable, denyGroups, pwdChangeInterval, pwdGracePeriod);

        Boolean recertify = getAttributeValue(attrs, RECERTIFY, Boolean.class);
        if (recertify != null && recertify) {
            AdministrationProcess adminProcess = connection.getAdministrationProcess();
            adminProcess.recertifyUser(fullName);
        }

        Integer mailQuotaSize = getAttributeValue(attrs, MAIL_QUOTA_SIZE_LIMIT, Integer.class);
        Integer mailWarningThreshold = getAttributeValue(attrs, MAIL_QUOTA_WARNING_THRESHOLD, Integer.class);
        if (mailQuotaSize != null || mailWarningThreshold != null) {
            String mailDb = document.getItemValueString(MAIL_FILE.getName());
            updateMailDbQuota(mailDb, mailQuotaSize, mailWarningThreshold);
        }

        updateAccountRenameUser(fullName, document, attrs);

        String mailServer = getAttributeValue(attrs, MAIL_SERVER);
        if (mailServer != null) {
            connection.getAdministrationProcess().moveMailUser(fullName, mailServer, NEW_HOME_SERVER_MAIL_PATH);
        }

        attrs.remove(CREDENTIALS.getName());
        attrs.remove(NORTH_AMERICAN.getName());

        for (Attribute attribute : attrs.values()) {
            Vector vector = new Vector(attribute.getValue());
            document.replaceItemValue(attribute.getName(), vector);
            changed = true;
        }

        if (changed && !document.save()) {
            LOG.ok("Couldn't update account for {0}.", fullName);
            throw new ConnectorException("Couldn't update account '" + fullName + "'.");
        }

        return uid;
    }

    private void setPassword(String fullName, String currentPassword, String newPassword, String idFile)
            throws NotesException {
        if ((currentPassword != null && newPassword == null)
                || (currentPassword == null && newPassword != null)) {
            LOG.error("Missing password.");
            throw new ConnectorException("Missing password.");
        }
//        if (config.getUseIDVault() && newPassword != null) {
//            Session session = connection.getSession();
//            session.resetUserPassword(null, fullName, newPassword);
//
//            resetPasswordInIDVault(fullName, newPassword);
//        } else if (currentPassword != null && newPassword != null) {
//            if (idFile == null) {
//                throw new ConnectorException("ID file is not defined.");
//            }
//
//            changeIdFilePassword(currentPassword, newPassword, idFile);
//        }
        if (config.getSyncInetPswd() && currentPassword != null && newPassword != null) {
            AdministrationProcess adminProcess = connection.getAdministrationProcess();
            adminProcess.changeHTTPPassword(fullName, currentPassword, newPassword);
        }
    }

    private void resetPasswordInIDVault(String username, String newPassword) {
        //todo implement using JNI or using LN script
    }

    private void changeIdFilePassword(String currentPassword, String newPassword, String idFile) {
        //todo implement using JNI or using LN script
    }

    private void updateAccountGroupList(String name, List<String> newGroups, Update update) throws NotesException {
        List<String> oldGroups = getGroupList(name);

        if (newGroups != null && !Update.REMOVE.equals(update)) {
            Vector users = new Vector();
            users.add(name);
            for (String newGroup : newGroups) {
                if (oldGroups == null || !oldGroups.contains(newGroup)) {
                    if (!checkIfGroupExists(newGroup)) {
                        LOG.error("Group {0} doesn't exist.", newGroup);
                        throw new ConnectorException("Group '" + newGroup + "' doesn't exist.");
                    }
                    connection.getAdministrationProcess().addGroupMembers(newGroup, users);
                }
                if (oldGroups != null) {
                    oldGroups.remove(newGroup);
                }
            }
        }

        if (oldGroups != null && !Update.ADD.equals(update)) {
            for (String oldGroup : oldGroups) {
                if (newGroups == null || !newGroups.contains(oldGroup) || !Update.REMOVE.equals(update)) {
                    continue;
                }

                removeUserFromGroup(name, oldGroup);
            }
        }
    }

    private void updateAccountRenameUser(String fullName, Document doc, Map<String, Attribute> attrs)
            throws NotesException {
        String lastName = getAttributeValue(attrs, LAST_NAME);
        String firstName = getAttributeValue(attrs, FIRST_NAME);
        String middleInitial = getAttributeValue(attrs, MIDDLE_INITIAL);
        String orgUnit = getAttributeValue(attrs, ORG_UNIT);
        String altCn = getAttributeValue(attrs, ALT_FULL_NAME);
        String altOU = getAttributeValue(attrs, ALT_ORG_UNIT);
        String altLanguage = getAttributeValue(attrs, ALT_FULL_NAME_LANGUAGE);

        if (firstName == null && middleInitial == null && lastName == null && orgUnit == null
                && altCn == null && altOU == null && altLanguage == null) {
            return;
        }

        firstName = firstName != null ? firstName : RENAME_NO_CHANGE;
        middleInitial = middleInitial != null ? middleInitial : RENAME_NO_CHANGE;
        lastName = lastName != null ? lastName : RENAME_NO_CHANGE;
        orgUnit = orgUnit != null ? orgUnit : getOrgUnit(connection, fullName);
        altCn = altCn != null ? getCommon(this.connection, altCn) : RENAME_NO_CHANGE;
        altOU = altOU != null ? altOU : RENAME_NO_CHANGE;
        altLanguage = altLanguage != null ? altLanguage : getDefaultValueForRename(ALT_FULL_NAME_LANGUAGE, doc);

        LOG.ok("updateAccountRenameUser: fullName {0}, lastName {1}, firstName {2}, middleInitial {3}, orgUnit {4}.",
                fullName, lastName, firstName, middleInitial, orgUnit);

        connection.getAdministrationProcess().renameNotesUser(fullName, lastName, firstName, middleInitial,
                orgUnit, altCn, altOU, altLanguage, false);
    }

    private String getDefaultValueForRename(DominoAccountAttribute attr, Document doc) throws NotesException {
        String value = doc.getItemValueString(attr.getName());
        if (value == null) {
            value = RENAME_NO_CHANGE;
        }

        return value;
    }

    private void updateMailDbQuota(String mailDb, Integer mailQuotaSize, Integer mailWarningThreshold) {
        if (mailDb != null) {
            return;
        }

        LOG.error("Quotas for mail db '" + mailDb + "' wont be changed, not yet implemented.");
        //todo implement through administration process or notes script or something...
    }

    private void updateAccountSecurity(String fullName, Boolean enabled, List<String> denyGroups,
                                       Integer pwdChangeInterval, Integer pwdGracePeriod) throws NotesException {
        LOG.ok("updateAccountSecurity for {0}, enabled {1}, denyGroups {2}, pwdChangeInterval {3}, pwdGracePeriod {4}",
                fullName, enabled, denyGroups, pwdChangeInterval, pwdGracePeriod);

        if (enabled != null) {
            updateAccountActivation(fullName, denyGroups, pwdChangeInterval, pwdGracePeriod, enabled);
        }

        if ((enabled != null && denyGroups != null && (pwdChangeInterval != null || pwdGracePeriod != null))
                || (pwdChangeInterval != null) || pwdGracePeriod != null) {
            connection.getAdministrationProcess().setUserPasswordSettings(fullName, null,
                    pwdChangeInterval, pwdGracePeriod, null);
        }
    }

    private void updateAccountActivation(String username, List<String> denyGroups, Integer pwdChIntervalInt,
                                         Integer pwdGracePeriodInt, boolean enabled) throws NotesException {
        AdministrationProcess adminProcess = connection.getAdministrationProcess();
        if (denyGroups != null) {
            for (String group : denyGroups) {
                if (enabled) {
                    removeUserFromGroup(username, group);
                } else {
                    Vector users = new Vector();
                    users.add(username);
                    adminProcess.addGroupMembers(group, users);
                }
            }
        } else {
            Integer pwdChk = enabled ? AdministrationProcess.PWD_CHK_DONTCHECKPASSWORD :
                    AdministrationProcess.PWD_CHK_LOCKOUT;
            adminProcess.setUserPasswordSettings(username,
                    pwdChk, pwdChIntervalInt, pwdGracePeriodInt, null);
        }
    }

    private void removeUserFromGroup(String usernameCanonical, String groupName)
            throws NotesException {
        LOG.ok("removeUserFromGroup: usernameCanonical {0}, groupName {1}", usernameCanonical, groupName);

        Document group = getGroup(groupName);
        if (group == null) {
            LOG.error("Invalid group name {0}.", groupName);
            throw new ConnectorException("Invalid group name '" + groupName + "'.");
        }

        Vector members = group.getItemValue(MEMBERS.getName());
        if (members != null && members.remove(usernameCanonical)) {
            Map<String, Attribute> attrs = new HashMap<String, Attribute>();
            attrs.put(MEMBERS.getName(), build(MEMBERS, members.toArray()));

            updateGroup(createGroupUid(group), attrs, null, Update.REPLACE);
        }
    }

    private Uid updateGroup(Uid uid, Map<String, Attribute> attrs, OperationOptions options, Update type)
            throws NotesException {
        LOG.ok("updateGroup {0}, attrs {1}, update {2}", uid, attrs, type);

        if (attrs.isEmpty()) {
            return uid;
        }

        attrs.remove(DISPLAY_NAME.getName());

        Name nameAtt = (Name) attrs.remove(Name.NAME);
        String name = nameAtt != null ? nameAtt.getNameValue() : null;
        String listName = getAttributeValue(attrs, LIST_NAME);

        if (nameAtt != null && listName != null && name != null && !name.equals(listName)) {
            throw new IllegalArgumentException("Name doesn't equal list name attribute.");
        }

        String fullName = name != null ? name : listName;

        Document document = getGroup(uid.getUidValue());
        if (document == null) {
            LOG.error("Invalid group uid {0}", uid);
            throw new ConnectorException("Invalid group uid '" + uid + "'.");
        }

        Vector members = getAttributeValue(attrs, MEMBERS, Vector.class);
        Vector memberGroups = getAttributeValue(attrs, MEMBER_GROUPS, Vector.class);
        Vector memberPeople = getAttributeValue(attrs, MEMBER_PEOPLE, Vector.class);

        for (Attribute attribute : attrs.values()) {
            document.replaceItemValue(attribute.getName(), getDominoValues(attribute));
        }
        if (fullName != null) {
            String mainName = getGroupDisplayName(fullName);
            if (!mainName.equals(uid.getUidValue())) {
                Document existingGroup = getGroup(mainName);
                if ((existingGroup != null) && (!existingGroup.getUniversalID().equals(document.getUniversalID()))) {
                    LOG.error("Group {0} already exists.", mainName);
                    throw new AlreadyExistsException("Group '" + mainName + "' already exists.");
                }
            }
            document.replaceItemValue(LIST_NAME.getName(), new Vector(Arrays.asList(fullName.split(";"))));

            uid = new Uid(fullName);
        }

        List<String> membersList = getGroupMembers(members, memberGroups, memberPeople);
        document.replaceItemValue(MEMBERS.getName(), new Vector(membersList));
        if (!document.save()) {
            LOG.error("Couldn't update group {0}.", fullName);
            throw new ConnectorException("Couldn't update group '" + fullName + "'.");
        }

        return uid;
    }

    public void delete(ObjectClass objClass, Uid uid, OperationOptions options) {
        try {
            if (ObjectClass.ACCOUNT.equals(objClass)) {
                deleteAccount(uid, options);
            } else if (ObjectClass.GROUP.equals(objClass)) {
                deleteGroup(uid, options);
            } else {
                LOG.ok("Unknown object class '{0}'.", objClass);
                throw new IllegalArgumentException("Unknown object class '" + objClass + "'.");
            }
        } catch (NotesException ex) {
            if (ex.id == NotesError.NOTES_ERR_BAD_UNID || ex.id == NotesError.NOTES_ERR_NOSUCH_GROUP) {
                throw new UnknownUidException(uid, objClass);
            }
            handleException(ex, "Couldn't delete " + objClass.getObjectClassValue() + " with uid " + uid, LOG);
        }
    }

    private void deleteAccount(Uid uid, OperationOptions options) throws NotesException {
        String unid = getGuid(uid.getUidValue());
        Document document = connection.getUserDatabase().getDocumentByUNID(unid);
        if (document == null) {
            throw new ConnectorException("Invalid uid '" + uid + "'.");
        }

        String userName = (String) document.getItemValue(FULL_NAME.getName()).elementAt(0);

        Integer mailFileAction = getOperationOptionValue(options, MAIL_FILE_ACTION, config.getMailFileAction());
        Boolean deleteWindowsUser = getOperationOptionValue(options, DELETE_WINDOWS_USER, false);

        AdministrationProcess adminProcess = connection.getAdministrationProcess();
        adminProcess.deleteUser(userName, config.getImmediateDelete(), mailFileAction,
                config.getDeleteDenyGroup(), deleteWindowsUser.booleanValue());
    }

    private void deleteGroup(Uid uid, OperationOptions options) throws NotesException {
        String groupName = getGroupDisplayName(uid.getUidValue());
        AdministrationProcess adminProcess = connection.getAdministrationProcess();
        adminProcess.deleteGroup(groupName, config.getImmediateDelete().booleanValue());
    }
}
