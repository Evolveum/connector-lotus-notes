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
import lotus.domino.NotesException;
import org.identityconnectors.common.IOUtil;
import org.identityconnectors.common.security.GuardedString;
import org.identityconnectors.framework.api.APIConfiguration;
import org.identityconnectors.framework.api.ConnectorFacade;
import org.identityconnectors.framework.api.ConnectorFacadeFactory;
import org.identityconnectors.framework.common.objects.*;
import org.identityconnectors.test.common.TestHelpers;
import org.testng.annotations.Test;

import java.io.FileInputStream;
import java.io.InputStream;
import java.util.HashSet;
import java.util.Properties;
import java.util.Set;

public class CopyHelperTest {

    @Test(enabled = false)
    public void copyTest() throws Exception {
        String config = "./src/test/resources/domino.properties";
        System.out.println("Using config: " + config);

        Properties properties = new Properties();
        InputStream input = null;
        try {
            input = new FileInputStream(config);
            properties.load(input);
        } catch (Exception ex) {
            System.out.println("Exception occurred during properties loading.");
            ex.printStackTrace();
        } finally {
            IOUtil.quietClose(input);
        }

        DominoConfiguration dCfgFrom = createDominoConfig(properties);
        dCfgFrom.setUserDatabaseName("names2.nsf");
        DominoConfiguration dCfgTo = createDominoConfig(properties);
        dCfgTo.setUserDatabaseName("names.nsf");

        final DominoConnection con = new DominoConnection(dCfgTo);

        ConnectorFacade from = getConnectorFacade(dCfgFrom);
        final ConnectorFacade to = getConnectorFacade(dCfgTo);

        //copying groups
        ResultsHandler handler = new ResultsHandler() {

            public boolean handle(ConnectorObject obj) {
                if (obj.getName().getNameValue().equals("LocalDomainAdmins")
                        || obj.getName().getNameValue().equals("LocalDomainServers")
                        || obj.getName().getNameValue().equals("OtherDomainServers")) {
                    System.out.println("Skipping: " + obj.getName());
                    return true;
                }

                System.out.println("Copying:" + obj.getName());
                try {
                    Set<Attribute> attrs = updateGroup(obj.getAttributes());
                    to.create(ObjectClass.GROUP, attrs, null);
                } catch (Exception ex) {
                    ex.printStackTrace();
                }
                return true;
            }
        };
        from.search(ObjectClass.GROUP, null, handler, null);

        //copyping accounts
        handler = new ResultsHandler() {

            private int count = 0;

            public boolean handle(ConnectorObject obj) {
                count++;

                try {
                    if (count % 25 == 0) {
                        System.out.println("Sleeping 10s...");
                        Thread.sleep(10000);
                    }
                } catch (Exception ex) {
                }

                if (obj.getName().getNameValue().equals("admin idm/example/SK")) {
                    System.out.println("Skipping: " + obj.getName());
                    return true;
                }

                System.out.println("Copying: " + obj);
                try {
                    to.create(ObjectClass.ACCOUNT, updateAccount(obj, con), null);
                } catch (Exception ex) {
                    ex.printStackTrace();
                }
                return true;
            }
        };
        from.search(ObjectClass.ACCOUNT, null, handler, null);

        ConnectorFacadeFactory factory = ConnectorFacadeFactory.getInstance();
        factory.dispose();
    }

    private Set<Attribute> updateAccount(ConnectorObject obj, DominoConnection con) throws NotesException {
        Set<Attribute> attr = obj.getAttributes();
        Set<Attribute> attrs = new HashSet<Attribute>();
        for (Attribute a : attr) {
            if (a.getName().equals(DominoAccountAttribute.CERTIFIER_ORG_HIERARCHY.getName())
                    || a.getName().equals(DominoAccountAttribute.ORG_UNIT.getName())) {
                continue;
            }
            attrs.add(a);
        }

        attrs.add(createAttribute(DominoAccountAttribute.CERTIFIER_ORG_HIERARCHY, "/example/SK"));
        attrs.add(createAttribute(DominoAccountAttribute.ORG_UNIT, DominoUtils.getOrgUnit(con, obj.getName().getNameValue())));

        String idFile = obj.getName().getNameValue().replaceAll(" ", "").replaceAll("/", "");
        attrs.add(createAttribute(DominoAccountAttribute.ID_FILE, "./target/id-folder/" + idFile + ".id"));
        attrs.add(AttributeBuilder.buildPassword("qwerty12345.X".toCharArray()));

        return attrs;
    }

    private Attribute createAttribute(DominoAttribute attr, Object... values) {
        return AttributeBuilder.build(attr.getName(), values);
    }

    private Set<Attribute> updateGroup(Set<Attribute> attr) {
        Set<Attribute> attrs = new HashSet<Attribute>();
        for (Attribute a : attr) {
            if (a.getName().equals(DominoGroupAttribute.MEMBERS.getName())
                    || a.getName().equals(DominoGroupAttribute.MEMBER_GROUPS.getName())
                    || a.getName().equals(DominoGroupAttribute.MEMBER_PEOPLE.getName())) {
                continue;
            }
            attrs.add(a);
        }

        return attrs;
    }

    private ConnectorFacade getConnectorFacade(DominoConfiguration config) {
        ConnectorFacadeFactory factory = ConnectorFacadeFactory.getInstance();
        APIConfiguration impl = TestHelpers.createTestConfiguration(DominoConnector.class, config);
        return factory.newInstance(impl);
    }

    private DominoConfiguration createDominoConfig(Properties properties) {
        DominoConfiguration dCfg = new DominoConfiguration();
        dCfg.setAdminName(properties.getProperty("adminName"));
        dCfg.setAdminPassword(new GuardedString(properties.getProperty("adminPassword", "").toCharArray()));
        dCfg.setIorHost(properties.getProperty("iorHost"));
        dCfg.setRegistrationServer(properties.getProperty("registrationServer"));

        dCfg.setAdministrationServer(dCfg.getRegistrationServer());
        dCfg.setCertifierIdFile("C:\\Program Files\\IBM\\Lotus\\Domino\\Data\\cert.id");
        dCfg.setCertifierPassword(new GuardedString("asdf.123".toCharArray()));
        dCfg.setCreateMailDbInBackground(true);
        dCfg.setDefaultPasswordExp(720);
        dCfg.setImmediateDelete(true);
        dCfg.setMailFileAction(1);
        dCfg.setMailOwnerAccess(2);
        dCfg.setMailServer("CN=server1/O=example/C=com");
        dCfg.setNorthAmerican(false);
        dCfg.setUserDatabaseName("names.nsf");
        dCfg.setStoreIDInAddressBook(false);
        dCfg.setStoreIDInMailfile(false);
        dCfg.setRegistrationLog("C:\\Program Files\\IBM\\Lotus\\Domino\\Data\\certlog.nsf");
        dCfg.setIdType(1);
        dCfg.setCreateIdFile(true);
        dCfg.setMailSystem(0);
        dCfg.setCreateMailDb(true);
        dCfg.setMinPasswordLength(6);
        dCfg.setUseCAProcess(false);
        dCfg.setMailACLManager("LocalDomainAdmins");
        dCfg.setUseCaseInsensitiveSearch(true);
        dCfg.setDisableDenyGroup("Terminators");
        dCfg.setUseIDVault(true);
        dCfg.setSyncInetPswd(true);

        return dCfg;
    }
}
