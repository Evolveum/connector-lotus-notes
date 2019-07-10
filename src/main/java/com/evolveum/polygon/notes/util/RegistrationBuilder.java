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

package com.evolveum.polygon.notes.util;


import lotus.domino.DateTime;
import lotus.domino.NotesException;
import lotus.domino.Registration;

import java.util.Vector;

public class RegistrationBuilder {

    private Registration registration;

    public RegistrationBuilder(Registration registration) {
        this.registration = registration;
    }

    public void setCertifierIDFile(String certifierIdFile) throws NotesException {
        if (certifierIdFile != null) {
            registration.setCertifierIDFile(certifierIdFile);
        }
    }

    public void setCreateMailDb(Boolean createMailDb) throws NotesException {
        if (createMailDb != null) {
            registration.setCreateMailDb(createMailDb);
        }
    }

    public void setExpiration(DateTime expiration) throws NotesException {
        if (expiration != null) {
            registration.setExpiration(expiration);
        }
    }

    public void setIDType(Integer idType) throws NotesException {
        if (idType != null) {
            registration.setIDType(idType);
        }
    }

    public void setMinPasswordLength(Integer minPasswordLength) throws NotesException {
        if (minPasswordLength != null) {
            registration.setMinPasswordLength(minPasswordLength);
        }
    }

    public void setOrgUnit(String orgUnit) throws NotesException {
        if (orgUnit != null) {
            registration.setOrgUnit(orgUnit);
        }
    }

    public void setRegistrationLog(String registrationLog) throws NotesException {
        if (registrationLog != null) {
            registration.setRegistrationLog(registrationLog);
        }
    }

    public void setRegistrationServer(String registrationServer) throws NotesException {
        if (registrationServer != null) {
            registration.setRegistrationServer(registrationServer);
        }
    }

    public void setStoreIDInAddressBook(Boolean storeIDInAddressBook) throws NotesException {
        if (storeIDInAddressBook != null) {
            registration.setStoreIDInAddressBook(storeIDInAddressBook);
        }
    }

    public void setNorthAmerican(Boolean northAmerican) throws NotesException {
        if (northAmerican != null) {
            registration.setNorthAmerican(northAmerican);
        }
    }

    public void setUpdateAddressBook(Boolean updateAddressBook) throws NotesException {
        if (updateAddressBook != null) {
            registration.setUpdateAddressBook(updateAddressBook);
        }
    }

    public void setAltOrgUnit(Vector altOrgUnit) throws NotesException {
        if (altOrgUnit != null) {
            registration.setAltOrgUnit(altOrgUnit);
        }
    }

    public void setAltOrgUnitLang(Vector altOrgUnitLang) throws NotesException {
        if (altOrgUnitLang != null) {
            registration.setAltOrgUnitLang(altOrgUnitLang);
        }
    }

    public void setCertifierName(String certifierName) throws NotesException {
        if (certifierName != null) {
            registration.setCertifierName(certifierName);
            registration.setUseCertificateAuthority(true);
        }
    }

    public void setMailACLManager(String mailACLManager) throws NotesException {
        if (mailACLManager != null) {
            registration.setMailACLManager(mailACLManager);
        }
    }

    public void setMailInternetAddress(String mailInternetAddress) throws NotesException {
        if (mailInternetAddress != null) {
            registration.setMailInternetAddress(mailInternetAddress);
        }
    }

    public void setMailTemplateName(String mailTemplateName) throws NotesException {
        if (mailTemplateName != null) {
            registration.setMailTemplateName(mailTemplateName);
        }
    }

    public void setPolicyName(String policyName) throws NotesException {
        if (policyName != null) {
            registration.setPolicyName(policyName);
        }
    }

    public void setRoamingServer(String roamingServer) throws NotesException {
        if (roamingServer != null) {
            registration.setRoamingServer(roamingServer);
        }
    }

    public void setRoamingSubdir(String roamingSubdir) throws NotesException {
        if (roamingSubdir != null) {
            registration.setRoamingSubdir(roamingSubdir);
        }
    }

    public void setShortName(String shortName) throws NotesException {
        if (shortName != null) {
            registration.setShortName(shortName);
        }
    }

    public void setEnforceUniqueShortName(Boolean enforceUniqueShortName) throws NotesException {
        if (enforceUniqueShortName != null) {
            registration.setEnforceUniqueShortName(enforceUniqueShortName);
        }
    }

    public void setRoamingUser(Boolean roamingUser) throws NotesException {
        if (roamingUser != null) {
            registration.setRoamingUser(roamingUser);
        }
    }

    public void setMailCreateFTIndex(Boolean mailCreateFTIndex) throws NotesException {
        if (mailCreateFTIndex != null) {
            registration.setMailCreateFTIndex(mailCreateFTIndex);
        }
    }

    public void setNoIDFile(Boolean noIDFile) throws NotesException {
        if (noIDFile != null) {
            registration.setNoIDFile(noIDFile);
        }
    }

    public void setStoreIDInMailfile(Boolean storeIDInMailfile) throws NotesException {
        if (storeIDInMailfile != null) {
            registration.setStoreIDInMailfile(storeIDInMailfile);
        }
    }

    public void setSynchInternetPassword(Boolean synchInternetPassword) throws NotesException {
        if (synchInternetPassword != null) {
            registration.setSynchInternetPassword(synchInternetPassword);
        }
    }

    public void setUseCertificateAuthority(Boolean useCertificateAuthority) throws NotesException {
        if (useCertificateAuthority != null) {
            registration.setUseCertificateAuthority(useCertificateAuthority);
        }
    }

    public void setMailQuotaSizeLimit(Integer mailQuotaSizeLimit) throws NotesException {
        if (mailQuotaSizeLimit != null) {
            registration.setMailQuotaSizeLimit(mailQuotaSizeLimit);
        }
    }

    public void setMailQuotaWarningThreshold(Integer mailQuotaWarningThreshold) throws NotesException {
        if (mailQuotaWarningThreshold != null) {
            registration.setMailQuotaWarningThreshold(mailQuotaWarningThreshold);
        }
    }

    public void setMailOwnerAccess(Integer mailOwnerAccess) throws NotesException {
        if (mailOwnerAccess != null) {
            registration.setMailOwnerAccess(mailOwnerAccess);
        }
    }

    public void setMailSystem(Integer mailSystem) throws NotesException {
        if (mailSystem != null) {
            registration.setMailSystem(mailSystem);
        }
    }

    public void setRoamingCleanupPeriod(Integer roamingCleanupPeriod) throws NotesException {
        if (roamingCleanupPeriod != null) {
            registration.setRoamingCleanupPeriod(roamingCleanupPeriod);
        }
    }

    public void setRoamingCleanupSetting(Integer roamingCleanupSetting) throws NotesException {
        if (roamingCleanupSetting != null) {
            registration.setRoamingCleanupSetting(roamingCleanupSetting);
        }
    }

    public void setGroupList(Vector groupList) throws NotesException {
        if (groupList != null) {
            registration.setGroupList(groupList);
        }
    }

    public void setMailReplicaServers(Vector mailReplicaServers) throws NotesException {
        if (mailReplicaServers != null) {
            registration.setMailReplicaServers(mailReplicaServers);
        }
    }

    public void setPublicKeySize(Integer publicKeySize) throws NotesException {
        if (publicKeySize != null) {
            registration.setPublicKeySize(publicKeySize);
        }
    }

    public void setForeignDN(String foreignDN) throws NotesException {
        if (foreignDN != null) {
            registration.setForeignDN(foreignDN);
        }
    }
}
