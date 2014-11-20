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

import lotus.domino.Registration;
import org.apache.commons.lang.StringUtils;
import org.identityconnectors.common.logging.Log;
import org.identityconnectors.common.security.GuardedString;
import org.identityconnectors.framework.common.exceptions.ConfigurationException;
import org.identityconnectors.framework.spi.AbstractConfiguration;
import org.identityconnectors.framework.spi.ConfigurationProperty;

public class DominoConfiguration extends AbstractConfiguration {

    private static final Log LOG = Log.getLog(DominoConfiguration.class);

    private String iorHost;
    private String adminName;
    private GuardedString adminPassword;

    private String userDatabaseName = "names.nsf";
    private String registrationServer;
    private String mailServer;
    private GuardedString certifierPassword;
    private String certifierIdFile = "C:\\Program Files\\IBM\\Lotus\\Domino\\Data\\cert.id";
    private String mailTemplateName;
    private String roamSrvr;
    private Integer roamCleanSetting;
    private Integer roamCleanPer;
    private String roamRplSrvrs;
    private Boolean createMailDbInBackground;
    private Integer defaultPasswordExp = 720;
    private Integer mailOwnerAccess;
    private String administrationServer;
    private Boolean immediateDelete;
    private Integer mailFileAction;
    private String deleteDenyGroup;
    private Boolean northAmerican = true;
    private Boolean storeIDInAddressBook = true;
    private Boolean storeIDInMailfile = false;
    private String registrationLog = "C:\\Program Files\\IBM\\Lotus\\Domino\\Data\\certlog.nsf";
    private Integer idType = 1;
    private Boolean createIdFile = true;
    private Integer mailSystem;
    private Boolean createMailDb = true;
    private String policy;
    private Integer minPasswordLength = 6;
    private Boolean useCAProcess = false;
    private String caCertifierName;
    private String mailACLManager;
    private Boolean useCaseInsensitiveSearch = true;
    private String disableDenyGroup;
    private Boolean useIDVault = true;
    private Boolean syncInetPswd = false;

    @Override
    public void validate() {
        if (getMailFileAction() < 0 || getMailFileAction() > 2) {
            throw new ConfigurationException("Mail file action must be from values 0 (none), 1 (home), 2 (all).");
        }

        if (getMailOwnerAccess() < 0 || getMailOwnerAccess() > 2) {
            throw new ConfigurationException("Mail owner access must be from values 0 (manager), 1 (designer), 2 (editor).");
        }

        if (getIdType() != null && (getIdType() < 0 || getIdType() > 2)) {
            throw new ConfigurationException("Possible values for ID type: 0 (flat), 1 (hierarchical), 2 (certifier).");
        }

        isNotEmpty(getIorHost(), "Ior host url must not be empty.");
        isNotEmpty(getUserDatabaseName(), "User database name must not be empty.");
        isNotEmpty(getAdministrationServer(), "Administration server must not be empty.");
        isNotEmpty(getRegistrationServer(), "Registration server must not be empty/");
    }

    private void isNotEmpty(String value, String exceptionMessage) {
        if (StringUtils.isEmpty(value)) {
            throw new ConfigurationException(exceptionMessage);
        }
    }

    @ConfigurationProperty(displayMessageKey = "UI_IOR_HOST",
            helpMessageKey = "UI_IOR_HOST_HELP")
    public String getIorHost() {
        return iorHost;
    }

    @ConfigurationProperty(displayMessageKey = "UI_ADMIN_PASSWORD",
            helpMessageKey = "UI_ADMIN_PASSWORD_HELP", confidential = true)
    public GuardedString getAdminPassword() {
        return adminPassword;
    }

    @ConfigurationProperty(displayMessageKey = "UI_ADMIN_NAME",
            helpMessageKey = "UI_ADMIN_NAME_HELP")
    public String getAdminName() {
        return adminName;
    }

    @ConfigurationProperty(displayMessageKey = "UI_REGISTRATION_SERVER",
            helpMessageKey = "UI_REGISTRATION_SERVER_HELP")
    public String getRegistrationServer() {
        return registrationServer;
    }

    @ConfigurationProperty(displayMessageKey = "UI_USER_DATABASE_NAME",
            helpMessageKey = "UI_USER_DATABASE_NAME_HELP")
    public String getUserDatabaseName() {
        return userDatabaseName;
    }

    @ConfigurationProperty(displayMessageKey = "UI_MAIL_SERVER",
            helpMessageKey = "UI_MAIL_SERVER_HELP")
    public String getMailServer() {
        return mailServer;
    }

    /**
     * The password of the certifier ID file.
     *
     * @return
     */
    @ConfigurationProperty(displayMessageKey = "UI_CERTIFIER_PASSWORD",
            helpMessageKey = "UI_CERTIFIER_PASSWORD_HELP")
    public GuardedString getCertifierPassword() {
        return certifierPassword;
    }

    /**
     * The file specification of the certifier ID file. The file specification can be a
     * complete file specification or can be relative to the Domino data directory.
     *
     * @return
     */
    @ConfigurationProperty(displayMessageKey = "UI_CERTIFIER_ID_FILE",
            helpMessageKey = "UI_CERTIFIER_ID_FILE_HELP")
    public String getCertifierIdFile() {
        return certifierIdFile;
    }

    @ConfigurationProperty(displayMessageKey = "UI_MAIL_TEMPLATE_NAME",
            helpMessageKey = "UI_MAIL_TEMPLATE_NAME_HELP")
    public String getMailTemplateName() {
        return mailTemplateName;
    }

    @ConfigurationProperty(displayMessageKey = "UI_ROAM_SRVR",
            helpMessageKey = "UI_ROAM_SRVR_HELP")
    public String getRoamSrvr() {
        return roamSrvr;
    }

    /**
     * Indicates the clean-up process for data on Notes clients set up for roaming users.
     * Available values:
     * <p/>
     * 0 - Never, (default) indicates that roaming data is never deleted.
     * 1 - Every N days, indicates that roaming data is deleted every so many days as specified by RoamingCleanupPeriod.
     * 2 - At shutdown, indicates that roaming data is deleted upon Notes shutdown.
     * 3 - Prompt, indicates that a user is prompted upon exiting Notes. The user can choose an individual clean-up or not. The user can also decline being prompted in the future.
     *
     * @see lotus.domino.Registration#REG_ROAMING_CLEANUP_NEVER
     * @see lotus.domino.Registration#REG_ROAMING_CLEANUP_EVERY_NDAYS
     * @see lotus.domino.Registration#REG_ROAMING_CLEANUP_AT_SHUTDOWN
     * @see lotus.domino.Registration#REG_ROAMING_CLEANUP_PROMPT
     */
    @ConfigurationProperty(displayMessageKey = "UI_ROAM_CLEAN_SETTING",
            helpMessageKey = "UI_ROAM_CLEAN_SETTING_HELP")
    public Integer getRoamCleanSetting() {
        return roamCleanSetting;
    }

    /**
     * The interval in days for cleaning up data on Notes clients set up for roaming users.
     *
     * @return
     */
    @ConfigurationProperty(displayMessageKey = "UI_ROAM_CLEAN_PER",
            helpMessageKey = "UI_ROAM_CLEAN_PER_HELP")
    public Integer getRoamCleanPer() {
        return roamCleanPer;
    }

    @ConfigurationProperty(displayMessageKey = "UI_ROAM_RPL_SRVRS",
            helpMessageKey = "UI_ROAM_RPL_SRVRS_HELP")
    public String getRoamRplSrvrs() {
        return roamRplSrvrs;
    }

    @ConfigurationProperty(displayMessageKey = "UI_CREATE_MAIL_DB_IN_BACKGROUND",
            helpMessageKey = "UI_CREATE_MAIL_DB_IN_BACKGROUND_HELP")
    public Boolean getCreateMailDbInBackground() {
        return createMailDbInBackground;
    }

    @ConfigurationProperty(displayMessageKey = "UI_DEFAULT_PASSWORD_EXP",
            helpMessageKey = "UI_DEFAULT_PASSWORD_EXP_HELP")
    public Integer getDefaultPasswordExp() {
        return defaultPasswordExp;
    }

    /**
     * The mail database ACL setting for the owner.
     * <p/>
     * 0 - Manager
     * 1 - Designer
     * 2 - Editor
     *
     * @return 0 by default
     * @see lotus.domino.Registration#REG_MAIL_OWNER_ACL_DESIGNER
     * @see lotus.domino.Registration#REG_MAIL_OWNER_ACL_EDITOR
     * @see lotus.domino.Registration#REG_MAIL_OWNER_ACL_MANAGER
     */
    @ConfigurationProperty(displayMessageKey = "UI_MAIL_OWNER_ACCESS",
            helpMessageKey = "UI_MAIL_OWNER_ACCESS_HELP")
    public Integer getMailOwnerAccess() {
        if (mailOwnerAccess == null) {
            mailOwnerAccess = 0;
        }
        return mailOwnerAccess;
    }

    @ConfigurationProperty(displayMessageKey = "UI_ADMINISTRATION_SERVER",
            helpMessageKey = "UI_ADMINISTRATION_SERVER_HELP")
    public String getAdministrationServer() {
        return administrationServer;
    }

    /**
     * True to delete all references to the user in the Domino Directory before issuing an
     * administration process request. False to let the administration process make all deletions.
     * <p/>
     * Note A true setting may impact performance.
     *
     * @return
     */
    @ConfigurationProperty(displayMessageKey = "UI_IMMEDIATE_DELETE",
            helpMessageKey = "UI_IMMEDIATE_DELETE_HELP")
    public Boolean getImmediateDelete() {
        if (immediateDelete == null) {
            immediateDelete = false;
        }

        return immediateDelete;
    }

    /**
     * Indicates the disposition of the user's mail file:
     * <p/>
     * 0 - None
     * 1 - Home
     * 2 - All
     *
     * @return 0 by default
     * @see lotus.domino.AdministrationProcess#MAILFILE_DELETE_ALL
     * @see lotus.domino.AdministrationProcess#MAILFILE_DELETE_HOME
     * @see lotus.domino.AdministrationProcess#MAILFILE_DELETE_NONE
     */
    @ConfigurationProperty(displayMessageKey = "UI_MAIL_FILE_ACTION",
            helpMessageKey = "UI_MAIL_FILE_ACTION_HELP")
    public Integer getMailFileAction() {
        if (mailFileAction == null) {
            mailFileAction = 0;
        }

        return mailFileAction;
    }

    /**
     * The name of an existing group of type "Deny List Only" to which the name of the
     * deleted user is added. The empty string means do not add the user name to any group.
     *
     * @return
     */
    @ConfigurationProperty(displayMessageKey = "UI_DELETE_DENY_GROUP",
            helpMessageKey = "UI_DELETE_DENY_GROUP_HELP")
    public String getDeleteDenyGroup() {
        return deleteDenyGroup;
    }

    @ConfigurationProperty(displayMessageKey = "UI_NORTH_AMERICAN",
            helpMessageKey = "UI_NORTH_AMERICAN_HELP")
    public Boolean getNorthAmerican() {
        return northAmerican;
    }

    @ConfigurationProperty(displayMessageKey = "UI_STORE_ID_IN_MAIL_FILE",
            helpMessageKey = "UI_STORE_ID_IN_MAIL_FILE_HELP")
    public Boolean getStoreIDInMailfile() {
        return storeIDInMailfile;
    }

    @ConfigurationProperty(displayMessageKey = "UI_STORE_ID_IN_ADDRESS_BOOK",
            helpMessageKey = "UI_STORE_ID_IN_ADDRESS_BOOK_HELP")
    public Boolean getStoreIDInAddressBook() {
        return storeIDInAddressBook;
    }

    @ConfigurationProperty(displayMessageKey = "UI_REGISTRATION_LOG",
            helpMessageKey = "UI_REGISTRATION_LOG_HELP")
    public String getRegistrationLog() {
        return registrationLog;
    }

    /**
     * Available values for ID type:
     * <p/>
     * 0 - Flat, to create a flat ID
     * 1 - Hierarchical, to create a hierarchical ID
     * 2 - Certifier, to create an ID that depends on whether the certifier ID is flat or hierarchical
     *
     * @return
     * @see Registration#ID_FLAT
     * @see Registration#ID_HIERARCHICAL
     * @see Registration#ID_CERTIFIER
     */
    @ConfigurationProperty(displayMessageKey = "UI_ID_TYPE",
            helpMessageKey = "UI_ID_TYPE_HELP")
    public Integer getIdType() {
        return idType;
    }

    @ConfigurationProperty(displayMessageKey = "UI_CREATE_ID_FILE",
            helpMessageKey = "UI_CREATE_ID_FILE_HELP")
    public Boolean getCreateIdFile() {
        return createIdFile;
    }

    /**
     * Available values for mail system:
     * <p/>
     * 0 - Notes
     * 1 - POP
     * 2 - IMAP
     * 3 - INotes
     * 4 - Internet
     * 5 - Other
     * 6 - None
     *
     * @return
     * @see Registration#REG_MAILSYSTEM_NOTES
     * @see Registration#REG_MAILSYSTEM_POP
     * @see Registration#REG_MAILSYSTEM_IMAP
     * @see Registration#REG_MAILSYSTEM_INOTES
     * @see Registration#REG_MAILSYSTEM_INTERNET
     * @see Registration#REG_MAILSYSTEM_OTHER
     * @see Registration#REG_MAILSYSTEM_NONE
     */
    @ConfigurationProperty(displayMessageKey = "UI_MAIL_SYSTEM",
            helpMessageKey = "UI_MAIL_SYSTEM_HELP")
    public Integer getMailSystem() {
        return mailSystem;
    }

    /**
     * Indicates whether a mail database is created with the ID file when creating user.
     * True to create a mail database, false to not create a mail database;
     * it will be created during setup.
     *
     * @return
     */
    @ConfigurationProperty(displayMessageKey = "UI_CREATE_MAIL_DB",
            helpMessageKey = "UI_CREATE_MAIL_DB_HELP")
    public Boolean getCreateMailDb() {
        return createMailDb;
    }

    @ConfigurationProperty(displayMessageKey = "UI_POLICY",
            helpMessageKey = "UI_POLICY_HELP")
    public String getPolicy() {
        return policy;
    }

    @ConfigurationProperty(displayMessageKey = "UI_MIN_PASSWORD_LENGTH",
            helpMessageKey = "UI_MIN_PASSWORD_LENGTH_HELP")
    public Integer getMinPasswordLength() {
        return minPasswordLength;
    }

    @ConfigurationProperty(displayMessageKey = "UI_CA_CERTIFIER_NAME",
            helpMessageKey = "UI_CA_CERTIFIER_NAME_HELP")
    public String getCaCertifierName() {
        return caCertifierName;
    }

    @ConfigurationProperty(displayMessageKey = "UI_USE_CA_PROCESS",
            helpMessageKey = "UI_USE_CA_PROCESS_HELP")
    public Boolean getUseCAProcess() {
        return useCAProcess;
    }

    @ConfigurationProperty(displayMessageKey = "UI_USE_MAIL_ACL_MANAGER",
            helpMessageKey = "UI_USE_MAIL_ACL_MANAGER_HELP")
    public String getMailACLManager() {
        return mailACLManager;
    }

    @ConfigurationProperty(displayMessageKey = "UI_USE_CASE_INSENSITIVE_SEARCH",
            helpMessageKey = "UI_USE_CASE_INSENSITIVE_SEARCH_HELP")
    public Boolean getUseCaseInsensitiveSearch() {
        return useCaseInsensitiveSearch;
    }

    @ConfigurationProperty(displayMessageKey = "UI_DISABLE_DENY_GROUP",
            helpMessageKey = "UI_DISABLE_DENY_GROUP_HELP")
    public String getDisableDenyGroup() {
        return disableDenyGroup;
    }

    @ConfigurationProperty(displayMessageKey = "UI_USE_ID_VAULT",
            helpMessageKey = "UI_USE_ID_VAULT_HELP")
    public Boolean getUseIDVault() {
        if (useIDVault == null) {
            useIDVault =  true;
        }
        return useIDVault;
    }

    @ConfigurationProperty(displayMessageKey = "UI_SYNC_INET_PSWD",
            helpMessageKey = "UI_SYNC_INET_PSWD_HELP")
    public Boolean getSyncInetPswd() {
        if (syncInetPswd == null) {
            syncInetPswd = false;
        }
        return syncInetPswd;
    }

    public void setSyncInetPswd(Boolean syncInetPswd) {
        this.syncInetPswd = syncInetPswd;
    }

    public void setUseIDVault(Boolean useIDVault) {
        this.useIDVault = useIDVault;
    }

    public void setDisableDenyGroup(String disableDenyGroup) {
        this.disableDenyGroup = disableDenyGroup;
    }

    public void setCaCertifierName(String caCertifierName) {
        this.caCertifierName = caCertifierName;
    }

    public void setUseCAProcess(Boolean useCAProcess) {
        this.useCAProcess = useCAProcess;
    }

    public void setMinPasswordLength(Integer minPasswordLength) {
        this.minPasswordLength = minPasswordLength;
    }

    public void setPolicy(String policy) {
        this.policy = policy;
    }

    public void setCreateMailDb(Boolean createMailDb) {
        this.createMailDb = createMailDb;
    }

    public void setMailSystem(Integer mailSystem) {
        this.mailSystem = mailSystem;
    }

    public void setCreateIdFile(Boolean createIdFile) {
        this.createIdFile = createIdFile;
    }

    public void setIdType(Integer idType) {
        this.idType = idType;
    }

    public void setStoreIDInAddressBook(Boolean storeIDInAddressBook) {
        this.storeIDInAddressBook = storeIDInAddressBook;
    }

    public void setStoreIDInMailfile(Boolean storeIDInMailfile) {
        this.storeIDInMailfile = storeIDInMailfile;
    }

    public void setRegistrationLog(String registrationLog) {
        this.registrationLog = registrationLog;
    }

    public void setAdminName(String adminName) {
        this.adminName = adminName;
    }

    public void setAdminPassword(GuardedString adminPassword) {
        this.adminPassword = adminPassword;
    }

    public void setIorHost(String iorHost) {
        this.iorHost = iorHost;
    }

    public void setRegistrationServer(String registrationServer) {
        this.registrationServer = registrationServer;
    }

    public void setUserDatabaseName(String userDatabaseName) {
        this.userDatabaseName = userDatabaseName;
    }

    public void setCertifierPassword(GuardedString certifierPassword) {
        this.certifierPassword = certifierPassword;
    }

    public void setMailServer(String mailServer) {
        this.mailServer = mailServer;
    }

    public void setCertifierIdFile(String certifierIdFile) {
        this.certifierIdFile = certifierIdFile;
    }

    public void setMailTemplateName(String mailTemplateName) {
        this.mailTemplateName = mailTemplateName;
    }

    public void setRoamCleanPer(Integer roamCleanPer) {
        this.roamCleanPer = roamCleanPer;
    }

    public void setRoamCleanSetting(Integer roamCleanSetting) {
        this.roamCleanSetting = roamCleanSetting;
    }

    public void setRoamSrvr(String roamSrvr) {
        this.roamSrvr = roamSrvr;
    }

    public void setRoamRplSrvrs(String roamRplSrvrs) {
        this.roamRplSrvrs = roamRplSrvrs;
    }

    public void setCreateMailDbInBackground(Boolean createMailDbInBackground) {
        this.createMailDbInBackground = createMailDbInBackground;
    }

    public void setAdministrationServer(String administrationServer) {
        this.administrationServer = administrationServer;
    }

    public void setDefaultPasswordExp(Integer defaultPasswordExp) {
        this.defaultPasswordExp = defaultPasswordExp;
    }

    public void setMailOwnerAccess(Integer mailOwnerAccess) {
        this.mailOwnerAccess = mailOwnerAccess;
    }

    public void setImmediateDelete(Boolean immediateDelete) {
        this.immediateDelete = immediateDelete;
    }

    public void setMailFileAction(Integer mailFileAction) {
        this.mailFileAction = mailFileAction;
    }

    public void setDeleteDenyGroup(String deleteDenyGroup) {
        this.deleteDenyGroup = deleteDenyGroup;
    }

    public void setNorthAmerican(Boolean northAmerican) {
        this.northAmerican = northAmerican;
    }

    public void setMailACLManager(String mailACLManager) {
        this.mailACLManager = mailACLManager;
    }

    public void setUseCaseInsensitiveSearch(Boolean useCaseInsensitiveSearch) {
        this.useCaseInsensitiveSearch = useCaseInsensitiveSearch;
    }

    Integer getRealIdType() {
        if (idType == null) {
            return null;
        }

        switch (getIdType()) {
            case 0:
                return Registration.ID_FLAT;
            case 1:
                return Registration.ID_HIERARCHICAL;
            case 2:
                return Registration.ID_CERTIFIER;
            default:
                return null;
        }
    }
}