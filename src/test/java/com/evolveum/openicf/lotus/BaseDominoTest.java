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

import org.apache.commons.lang.StringUtils;
import org.identityconnectors.common.IOUtil;
import org.identityconnectors.common.logging.Log;
import org.identityconnectors.common.security.GuardedString;
import org.identityconnectors.framework.api.APIConfiguration;
import org.identityconnectors.framework.api.ConnectorFacade;
import org.identityconnectors.framework.api.ConnectorFacadeFactory;
import org.identityconnectors.test.common.TestHelpers;
import org.testng.AssertJUnit;
import org.testng.annotations.AfterClass;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.BeforeMethod;

import java.io.FileInputStream;
import java.io.InputStream;
import java.lang.reflect.Method;
import java.util.Properties;

public class BaseDominoTest {

    public static final String PROPERTY_CONFIG = "config";

    private static final Log LOG = Log.getLog(BaseDominoTest.class);

    private DominoConnector connector;

    @BeforeClass
    public final void beforeClass() throws Exception {
        Log log = Log.getLog(getClass());
        log.info(">>>>>>>>>>>>>>>>>>>>>>>> START {0} <<<<<<<<<<<<<<<<<<<<<<<<", new Object[]{getClass().getName()});

        customBeforeClass();
    }

    @AfterClass
    public final void afterClass() throws Exception {
        customAfterClass();

        Log log = Log.getLog(getClass());
        log.info(">>>>>>>>>>>>>>>>>>>>>>>> FINISH {0} <<<<<<<<<<<<<<<<<<<<<<<<", new Object[]{getClass().getName()});
    }

    @BeforeMethod
    public final void beforeMethod(Method method) throws Exception {
        Log log = Log.getLog(getClass());
        log.info(">>>>>>>>>>>>>>>>>>>>>>>> START {0}.{1} <<<<<<<<<<<<<<<<<<<<<<<<", new Object[]{getClass().getName(), method.getName()});

        customBeforeMethod(method);
    }

    @AfterMethod
    public final void afterMethod(Method method) throws Exception {
        customAfterMethod(method);

        Log log = Log.getLog(getClass());
        log.info(">>>>>>>>>>>>>>>>>>>>>>>> END {0}.{1} <<<<<<<<<<<<<<<<<<<<<<<<", new Object[]{getClass().getName(), method.getName()});
    }

    protected void customAfterMethod(Method method) throws Exception {
        disposeConnector();
    }

    protected void customBeforeClass() throws Exception {

    }

    protected void customBeforeMethod(Method method) throws Exception {

    }

    protected void customAfterClass() throws Exception {
        disposeConnectorFactory();
    }

    protected void disposeConnector() {
        if (connector != null) {
            connector.dispose();
            connector = null;
        }
    }

    protected void disposeConnectorFactory() {
        ConnectorFacadeFactory factory = ConnectorFacadeFactory.getInstance();
        factory.dispose();
    }

    protected DominoConfiguration createConfig() {
        String config = System.getProperty(PROPERTY_CONFIG);
        if (StringUtils.isEmpty(config)) {
            config = "./src/test/resources/domino.properties";
            LOG.info("System property '" + PROPERTY_CONFIG
                    + "' not defined, looking for default config '" + config + "'.");
        }

        Properties properties = new Properties();
        InputStream input = null;
        try {
            input = new FileInputStream(config);
            properties.load(input);
        } catch (Exception ex) {
            LOG.info(ex, "Exception occurred during properties loading.");
            AssertJUnit.fail("Exception occurred during properties loading, reason");
        } finally {
            IOUtil.quietClose(input);
        }

        DominoConfiguration dominoConfig = new DominoConfiguration();
        dominoConfig.setAdminName(properties.getProperty("adminName"));
        dominoConfig.setAdminPassword(new GuardedString(properties.getProperty("adminPassword", "").toCharArray()));
        dominoConfig.setIorHost(properties.getProperty("iorHost"));
        dominoConfig.setRegistrationServer(properties.getProperty("registrationServer"));
        dominoConfig.setUserDatabaseName(properties.getProperty("userDatabaseName"));

        dominoConfig.setAdministrationServer(dominoConfig.getRegistrationServer());
        dominoConfig.setCertifierIdFile("C:\\Program Files\\IBM\\Lotus\\Domino\\Data\\cert.id");
        dominoConfig.setCertifierPassword(new GuardedString("asdf.123".toCharArray()));
        dominoConfig.setCreateMailDbInBackground(true);
        dominoConfig.setDefaultPasswordExp(720);
        dominoConfig.setImmediateDelete(true);
        dominoConfig.setMailFileAction(1);
        dominoConfig.setMailOwnerAccess(2);
        dominoConfig.setMailServer("CN=server1/O=example/C=com");
        dominoConfig.setNorthAmerican(false);
        dominoConfig.setUserDatabaseName("names.nsf");
        dominoConfig.setStoreIDInAddressBook(false);
        dominoConfig.setStoreIDInMailfile(false);
        dominoConfig.setRegistrationLog("C:\\Program Files\\IBM\\Lotus\\Domino\\Data\\certlog.nsf");
        dominoConfig.setIdType(1);
        dominoConfig.setCreateIdFile(true);
        dominoConfig.setMailSystem(0);
        dominoConfig.setCreateMailDb(true);
        dominoConfig.setMinPasswordLength(6);
        dominoConfig.setUseCAProcess(false);
        dominoConfig.setMailACLManager("LocalDomainAdmins");
        dominoConfig.setUseCaseInsensitiveSearch(true);
        dominoConfig.setDisableDenyGroup("Terminators");
        dominoConfig.setUseIDVault(true);
        dominoConfig.setSyncInetPswd(true);

//        dominoConfig.setCaCertifierName(dominoConfig.getAdminName());
//        dominoConfig.setDeleteDenyGroup();
//        dominoConfig.setPolicy();
//        dominoConfig.setMailTemplateName();
//        dominoConfig.setRoamCleanPer();
//        dominoConfig.setRoamCleanSetting();
//        dominoConfig.setRoamRplSrvrs();

        return dominoConfig;
    }

    protected DominoConnector createConnectorInstance() {
        DominoConnector connector = new DominoConnector();
        DominoConfiguration config = createConfig();
        connector.init(config);

        return connector;
    }

    protected DominoConnector getConnector() {
        if (connector == null) {
            connector = createConnectorInstance();
        }
        return connector;
    }

    protected ConnectorFacade getConnectorFacade() {
        ConnectorFacadeFactory factory = ConnectorFacadeFactory.getInstance();
        APIConfiguration impl = TestHelpers.createTestConfiguration(DominoConnector.class, createConfig());
        return factory.newInstance(impl);
    }
}
