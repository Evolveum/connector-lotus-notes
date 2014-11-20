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

package com.evolveum.openicf.lotus;

import org.identityconnectors.common.security.GuardedString;
import org.identityconnectors.framework.common.objects.AttributeInfo;
import org.identityconnectors.framework.common.objects.AttributeInfoBuilder;
import org.identityconnectors.framework.common.objects.OperationalAttributeInfos;
import org.identityconnectors.framework.common.objects.OperationalAttributes;

import java.util.*;

import static org.identityconnectors.framework.common.objects.AttributeInfo.Flags.*;

public enum DominoAccountAttribute implements DominoAttribute {

    ALT_FULL_NAME("AltFullName"),
    ALT_FULL_NAME_LANGUAGE("AltFullNameLanguage"),
    ALT_ORG_UNIT("AltOrgUnit", String.class, MULTIVALUED),
    ASSISTANT("Assistant"),
    CA_CERTIFIER("CACertifier"),
    CALENDAR_DOMAIN("CalendarDomain"),
    CELL_PHONE_NUMBER("CellPhoneNumber"),
    CERTIFIER_ID_FILE("certifierIDFile", String.class, NOT_READABLE, NOT_RETURNED_BY_DEFAULT),
    CERTIFIER_ORG_HIERARCHY("CertifierOrgHierarchy", String.class, NOT_RETURNED_BY_DEFAULT),
    /**
     * 0 - does not require the user to enter a password when authenticating with other servers (default)
     * 1 - requires the user to enter a password when authenticating with servers that have password checking enabled
     * 2 - prevents the user from accessing servers that have password checking enabled
     *
     * @see lotus.domino.AdministrationProcess#PWD_CHK_CHECKPASSWORD
     * @see lotus.domino.AdministrationProcess#PWD_CHK_DONTCHECKPASSWORD
     * @see lotus.domino.AdministrationProcess#PWD_CHK_LOCKOUT
     */
    CHECK_PASSWORD("CheckPassword"),
    CHILDREN("Children"),
    CITY("City"),
    COMMENT("Comment"),
    COMPANY_NAME("CompanyName"),
    COUNTRY("country"),
    CREDENTIALS("credentials", GuardedString.class, NOT_READABLE, NOT_RETURNED_BY_DEFAULT),
    DEFAULT_PASSWORD_EXP("defaultPasswordExp", Integer.class, NOT_READABLE, NOT_RETURNED_BY_DEFAULT),
    DENY_GROUPS("DenyGroups"),
    DEPARTMENT("Department"),
    EMPLOYEE_ID("EmployeeID"),
    END_DATE("EndDate", Integer.class, NOT_READABLE, NOT_RETURNED_BY_DEFAULT),
    FIRST_NAME("FirstName"),
    FORWARDING_ADDRESS("forwardingAddress", String.class, NOT_RETURNED_BY_DEFAULT),
    FULL_NAME("FullName"),
    GROUP_LIST("GroupList", String.class, MULTIVALUED),
    HOME_FAX_PHONE_NUMBER("HomeFAXPhoneNumber"),
    HTTP_PASSWORD("HTTPPassword", GuardedString.class, NOT_READABLE, NOT_RETURNED_BY_DEFAULT),
    ID_FILE("idFile", String.class, REQUIRED, NOT_UPDATEABLE, NOT_READABLE, NOT_RETURNED_BY_DEFAULT),
    INTERNET_ADDRESS("InternetAddress"),
    JOB_TITLE("JobTitle"),
    LAST_MODIFIED("LastModified", Long.class, NOT_UPDATEABLE, NOT_CREATABLE),
    LAST_NAME("LastName"),
    LOCAL_ADMIN("LocalAdmin", String.class),
    LOCATION("Location"),
    MAIL_ADDRESS("MailAddress"),
    MAIL_DOMAIN("MailDomain"),
    MAIL_FILE("MailFile"),
    MAIL_QUOTA_WARNING_THRESHOLD("MailQuotaWarningThreshold", Integer.class),
    MAIL_QUOTA_SIZE_LIMIT("MailQuotaSizeLimit", Integer.class),
    MAIL_REPLICA_SERVERS("MailReplicaServers", String.class, NOT_UPDATEABLE, NOT_READABLE, NOT_RETURNED_BY_DEFAULT, MULTIVALUED),
    MAIL_SERVER("MailServer"),
    MAIL_TEMPLATE_NAME("MailTemplateName"),
    MANAGER("Manager"),
    MIDDLE_INITIAL("MiddleInitial"),
    NET_USER_NAME("NetUserName"),
    NORTH_AMERICAN("NorthAmerican", Boolean.class, NOT_READABLE, NOT_RETURNED_BY_DEFAULT),
    OBJECT_GUID("objectGUID", String.class, NOT_UPDATEABLE, NOT_CREATABLE),
    OFFICE_CITY("OfficeCity"),
    OFFICE_COUNTRY("OfficeCountry"),
    OFFICE_FAX_PHONE_NUMBER("OfficeFAXPhoneNumber"),
    OFFICE_NUMBER("OfficeNumber"),
    OFFICE_PHONE_NUMBER("OfficePhoneNumber"),
    OFFICE_STATE("OfficeState"),
    OFFICE_STREET_ADDRESS("OfficeStreetAddress"),
    OFFICE_ZIP("OfficeZIP"),
    ORG_UNIT("OrgUnit"),
    PASSWORD_CHANGE_INTERVAL("PasswordChangeInterval", Integer.class),
    PASSWORD_GRACE_PERIOD("PasswordGracePeriod", Integer.class),
    PHONE_NUMBER_6("PhoneNumber_6"),
    PHONE_NUMBER("PhoneNumber"),
    POLICY("Policy"),
    PROFILES("Profiles"),
    RECERTIFY("Recertify", Boolean.class, NOT_READABLE, NOT_RETURNED_BY_DEFAULT),
    ROAM_CLEAN_PER("RoamCleanPer", Integer.class),
    /**
     *
     * 0 - never
     * 1 - every n days
     * 2 - at shutdown
     * 3 - prompt
     *
     * @see lotus.domino.Registration#REG_ROAMING_CLEANUP_NEVER
     * @see lotus.domino.Registration#REG_ROAMING_CLEANUP_EVERY_NDAYS
     * @see lotus.domino.Registration#REG_ROAMING_CLEANUP_AT_SHUTDOWN
     * @see lotus.domino.Registration#REG_ROAMING_CLEANUP_PROMPT
     *
     */
    ROAM_CLEAN_SETTING("RoamCleanSetting", Integer.class),
    ROAM_RPL_SRVRS("RoamRplSrvrs"),
    ROAM_SRVR("RoamSrvr"),
    ROAM_SUBDIR("RoamSubdir"),
    SAMETIME_SERVER("SametimeServer"),
    SHORT_NAME("ShortName"),
    SPOUSE("Spouse"),
    STATE("State"),
    STREET_ADDRESS("StreetAddress"),
    SUFFIX("Suffix"),
    TITLE("Title"),
    WEB_SITE("WebSite"),
    X400_ADDRESS("x400Address"),
    ZIP("Zip"),

    // OPERATIONAL ATTRIBUTES
    CURRENT_PASSWORD(AttributeInfoBuilder.build(OperationalAttributes.CURRENT_PASSWORD_NAME, GuardedString.class,
            EnumSet.of(NOT_READABLE, NOT_RETURNED_BY_DEFAULT, NOT_CREATABLE))),
    ENABLE(OperationalAttributeInfos.ENABLE),
    PASSWORD(OperationalAttributeInfos.PASSWORD);

    private static final Map<String, DominoAccountAttribute> ATTRIBUTE_MAP = new HashMap<String, DominoAccountAttribute>();

    static {
        for (DominoAccountAttribute attr : DominoAccountAttribute.values()) {
            ATTRIBUTE_MAP.put(attr.getName(), attr);
        }
    }

    private String name;
    private Class type;
    private Set<AttributeInfo.Flags> flags;

    private AttributeInfo attribute;

    private DominoAccountAttribute(AttributeInfo attribute) {
        this.attribute = attribute;
    }

    private DominoAccountAttribute(String name) {
        this(name, String.class);
    }

    private DominoAccountAttribute(String name, Class type, AttributeInfo.Flags... flags) {
        this.name = name;
        this.type = type;

        Set<AttributeInfo.Flags> set = new HashSet<AttributeInfo.Flags>();
        for (AttributeInfo.Flags flag : flags) {
            set.add(flag);
        }
        this.flags = Collections.unmodifiableSet(set);
    }

    public Set<AttributeInfo.Flags> getFlags() {
        if (attribute != null) {
            return attribute.getFlags();
        }
        return flags;
    }

    public String getName() {
        if (attribute != null) {
            return attribute.getName();
        }
        return name;
    }

    public Class getType() {
        if (attribute != null) {
            return attribute.getType();
        }
        return type;
    }

    public boolean isOperational() {
        return attribute != null;
    }

    public AttributeInfo getAttribute() {
        return attribute;
    }

    public static DominoAccountAttribute getAttribute(String name) {
        return ATTRIBUTE_MAP.get(name);
    }
}
