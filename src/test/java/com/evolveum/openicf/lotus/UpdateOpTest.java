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
import org.identityconnectors.framework.common.objects.filter.EqualsFilter;
import org.testng.AssertJUnit;
import org.testng.annotations.Test;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class UpdateOpTest extends BaseDominoTest {

    private static final String GROUP_1 = "grp1";
    private static final String GROUP_2 = "grp2";

    @Test
    public void testGroupsUpdate() throws Exception {
        ConnectorFacade connector = getConnectorFacade();

        //create account
        Set<Attribute> attrs = CreateOpTest.createAttrsForJohnDoe(219);
        attrs.add(AttributeBuilder.build(DominoAccountAttribute.GROUP_LIST.getName(), GROUP_1));
        Uid uid = connector.create(ObjectClass.ACCOUNT, attrs, null);
        AssertJUnit.assertNotNull(uid);
        //validate groups
        validateGroupList(connector, uid, AttributeBuilder.build(DominoAccountAttribute.GROUP_LIST.getName(), GROUP_1));
        //add group
        attrs = new HashSet<Attribute>();
        Attribute newAttr = AttributeBuilder.build(DominoAccountAttribute.GROUP_LIST.getName(), GROUP_2);
        attrs.add(newAttr);
        Uid uid1 = connector.addAttributeValues(ObjectClass.ACCOUNT, uid, attrs, null);
        AssertJUnit.assertEquals(uid, uid1);
        //validate groups
        validateGroupList(connector, uid, AttributeBuilder.build(DominoAccountAttribute.GROUP_LIST.getName(), GROUP_1, GROUP_2));
        //remove group
        attrs = new HashSet<Attribute>();
        newAttr = AttributeBuilder.build(DominoAccountAttribute.GROUP_LIST.getName(), GROUP_1);
        attrs.add(newAttr);
        uid1 = connector.removeAttributeValues(ObjectClass.ACCOUNT, uid, attrs, null);
        AssertJUnit.assertEquals(uid, uid1);
        //validate groups
        validateGroupList(connector, uid, AttributeBuilder.build(DominoAccountAttribute.GROUP_LIST.getName(), GROUP_2));
    }

    @Test
    public void testRemoveGroup() throws Exception {
        ConnectorFacade connector = getConnectorFacade();

        //create account
        Set<Attribute> attrs = CreateOpTest.createAttrsForJohnDoe(218);
        attrs.add(AttributeBuilder.build(DominoAccountAttribute.GROUP_LIST.getName(), GROUP_1, GROUP_2));
        Uid uid = connector.create(ObjectClass.ACCOUNT, attrs, null);
        AssertJUnit.assertNotNull(uid);
        //validate groups
//        validateGroupList(connector, uid, AttributeBuilder.build(DominoAccountAttribute.GROUP_LIST.getName(), GROUP_1, GROUP_2));

        //remove group
        attrs = new HashSet<Attribute>();
        Attribute newAttr = AttributeBuilder.build(DominoAccountAttribute.GROUP_LIST.getName(), GROUP_1);
        attrs.add(newAttr);
        Uid uid1 = connector.removeAttributeValues(ObjectClass.ACCOUNT, uid, attrs, null);
        AssertJUnit.assertEquals(uid, uid1);
        //validate groups
        validateGroupList(connector, uid, AttributeBuilder.build(DominoAccountAttribute.GROUP_LIST.getName(), GROUP_2));
    }

    private void validateGroupList(ConnectorFacade connector, Uid uid, Attribute expected) {
        try {
            Thread.sleep(35000);
        } catch (Exception ex) {
        }

        ConnectorObject obj = connector.getObject(ObjectClass.ACCOUNT, uid, null);
        AssertJUnit.assertNotNull(obj);

        Attribute attr = obj.getAttributeByName(DominoAccountAttribute.GROUP_LIST.getName());
        if (expected == null) {
            AssertJUnit.assertNull(attr);
        } else {
            AssertJUnit.assertNotNull(attr);
            AssertJUnit.assertEquals(expected, attr);
        }
    }

    private void assertEnable(ConnectorFacade connector, Uid uid, Boolean enabled) {
        ConnectorObject obj = connector.getObject(ObjectClass.ACCOUNT, uid, null);
        AssertJUnit.assertNotNull(obj);

        Attribute attr = obj.getAttributeByName(OperationalAttributes.ENABLE_NAME);
        AssertJUnit.assertNotNull(attr);
        Boolean value = AttributeUtil.getBooleanValue(attr);
        AssertJUnit.assertEquals(enabled, value);

        attr = obj.getAttributeByName(DominoAccountAttribute.GROUP_LIST.getName());
        if (enabled) {
            if (attr != null) {
                List values = attr.getValue();
                AssertJUnit.assertTrue(!values.contains(createConfig().getDisableDenyGroup()));
            }
        } else {
            AssertJUnit.assertNotNull(attr);
            List values = attr.getValue();
            AssertJUnit.assertTrue(!values.contains(createConfig().getDisableDenyGroup()));
        }
    }

    @Test
    public void testEnable() throws Exception {
        ConnectorFacade connector = getConnectorFacade();

        //create account
        Set<Attribute> attrs = CreateOpTest.createAttrsForJohnDoe(226);
        Uid uid = connector.create(ObjectClass.ACCOUNT, attrs, null);
        AssertJUnit.assertNotNull(uid);
        assertEnable(connector, uid, true);

        attrs = new HashSet<Attribute>();
        attrs.add(AttributeBuilder.buildEnabled(false));
        connector.update(ObjectClass.ACCOUNT, uid, attrs, null);
        assertEnable(connector, uid, false);
    }

    @Test
    public void testGet() throws Exception {
        ConnectorFacade connector = getConnectorFacade();

        final List<ConnectorObject> objects = new ArrayList<ConnectorObject>();
        ResultsHandler handler = new ResultsHandler() {

            public boolean handle(ConnectorObject obj) {
                objects.add(obj);
                return true;
            }
        };

        connector.search(ObjectClass.ACCOUNT, new EqualsFilter(
                AttributeBuilder.build(Name.NAME, "John Doe/1234/example/COM")), handler, null);

        System.out.println(objects.get(0));
    }

    @Test
    public void testRename() throws Exception {
        ConnectorFacade connector = getConnectorFacade();

        //create account
        Set<Attribute> attrs = CreateOpTest.createAttrsForJohnDoe(235);
        Uid uid = connector.create(ObjectClass.ACCOUNT, attrs, null);
        AssertJUnit.assertNotNull(uid);

        attrs = new HashSet<Attribute>();
        Attribute newAttr = AttributeBuilder.build(DominoAccountAttribute.LAST_NAME.getName(), "Doe2");
        attrs.add(newAttr);
        connector.update(ObjectClass.ACCOUNT, uid, attrs, null);

    }

    @Test
    public void testUpdatePassword() throws Exception {
        Set<Attribute> attrs = CreateOpTest.createAttrsForJohnDoe(304);
        ConnectorFacade connector = getConnectorFacade();
        Uid uid = connector.create(ObjectClass.ACCOUNT, attrs, null);

        ConnectorObject con = connector.getObject(ObjectClass.ACCOUNT, uid, null);
        System.out.println(con);

        attrs = new HashSet<Attribute>();
        attrs.add(AttributeBuilder.buildCurrentPassword("asdf.123".toCharArray()));
        attrs.add(AttributeBuilder.buildPassword("zxcv.1234".toCharArray()));

        connector.update(ObjectClass.ACCOUNT, uid, attrs, null);

        con = connector.getObject(ObjectClass.ACCOUNT, uid, null);
        System.out.println(con);
    }
}
