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

import org.identityconnectors.framework.api.ConnectorFacade;
import org.identityconnectors.framework.common.objects.*;
import org.identityconnectors.framework.common.objects.filter.ContainsAllValuesFilter;
import org.identityconnectors.framework.common.objects.filter.EqualsFilter;
import org.identityconnectors.framework.common.objects.filter.Filter;
import org.testng.AssertJUnit;
import org.testng.annotations.Test;

import java.util.ArrayList;
import java.util.List;

public class SearchOpTest extends BaseDominoTest {

    @Test
    public void searchJDoe() throws Exception {
        ConnectorFacade connector = getConnectorFacade();

        Attribute attr = AttributeBuilder.build(DominoAccountAttribute.SHORT_NAME.getName(), "jDoe", "sn0001");
        Filter filter = new ContainsAllValuesFilter(attr);

        final List<ConnectorObject> objects = new ArrayList<ConnectorObject>();
        ResultsHandler handler = new ResultsHandler() {
            public boolean handle(ConnectorObject obj) {
                objects.add(obj);
                return true;
            }
        };
        connector.search(ObjectClass.ACCOUNT, filter, handler, null);

        System.out.println(objects);

        AssertJUnit.assertEquals(1, objects.size());
    }

    @Test
    public void simpleAccountSearch() throws Exception {
        ConnectorFacade connector = getConnectorFacade();

        final List<ConnectorObject> objects = new ArrayList<ConnectorObject>();
        ResultsHandler handler = new ResultsHandler() {
            public boolean handle(ConnectorObject obj) {
                objects.add(obj);
                return false;
            }
        };
        connector.search(ObjectClass.ACCOUNT, null, handler, null);

        System.out.println(objects);
    }

    @Test
    public void simpleGroupSearch() throws Exception {
        DominoConnector connector = getConnector();

        final List<ConnectorObject> objects = new ArrayList<ConnectorObject>();
        ResultsHandler handler = new ResultsHandler() {
            public boolean handle(ConnectorObject obj) {
                objects.add(obj);
                return false;
            }
        };
        connector.executeQuery(ObjectClass.GROUP, null, handler, null);

        System.out.println(objects);
    }

    @Test
    public void getUser() throws Exception {
        ConnectorFacade connector = getConnectorFacade();

        Uid uid = new Uid("3F098FFD50739BA6C1257B2E004AE2AA");
        ConnectorObject obj = connector.getObject(ObjectClass.ACCOUNT, uid, null);
        System.out.println(obj);
        AssertJUnit.assertNotNull(obj);
    }

    @Test
    public void queryAccount() throws Exception {
        ConnectorFacade connector = getConnectorFacade();

        final List<ConnectorObject> objects = new ArrayList<ConnectorObject>();
        ResultsHandler handler = new ResultsHandler() {
            public boolean handle(ConnectorObject obj) {
                objects.add(obj);
                return true;
            }
        };
        Filter filter = new EqualsFilter(new Name("John Doe/1234/example/com"));
        connector.search(ObjectClass.ACCOUNT, filter, handler, null);

        System.out.println(objects);
    }

    @Test
    public void queryUserWithGroups() throws Exception {
        ConnectorFacade connector = getConnectorFacade();

        final List<ConnectorObject> objects = new ArrayList<ConnectorObject>();
        ResultsHandler handler = new ResultsHandler() {
            public boolean handle(ConnectorObject obj) {
                objects.add(obj);
                return true;
            }
        };
        Filter filter = new EqualsFilter(new Name("John Doe/7777/example/COM"));
        connector.search(ObjectClass.ACCOUNT, filter, handler, null);

        AssertJUnit.assertEquals(1, objects.size());
        ConnectorObject object = objects.get(0);
        Attribute groupList = object.getAttributeByName(DominoAccountAttribute.GROUP_LIST.getName());
        AssertJUnit.assertNotNull(groupList);

        System.out.println(object);
    }
}
