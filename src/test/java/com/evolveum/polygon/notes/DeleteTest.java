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

import org.identityconnectors.framework.api.ConnectorFacade;
import org.identityconnectors.framework.common.objects.Attribute;
import org.identityconnectors.framework.common.objects.ConnectorObject;
import org.identityconnectors.framework.common.objects.ObjectClass;
import org.identityconnectors.framework.common.objects.Uid;
import org.testng.AssertJUnit;
import org.testng.annotations.Test;

import java.util.Set;

public class DeleteTest extends BaseDominoTest {

    @Test
    public void simpleCreateDelete() {
        ConnectorFacade connector = getConnectorFacade();

        Set<Attribute> attrs = CreateOpTest.createAttrsForJohnDoe(116);
        final Uid uid = connector.create(ObjectClass.ACCOUNT, attrs, null);
        AssertJUnit.assertNotNull(uid);

        ConnectorObject obj = connector.getObject(ObjectClass.ACCOUNT, uid, null);
        AssertJUnit.assertNotNull(obj);

        connector.delete(ObjectClass.ACCOUNT, uid, null);

        obj = connector.getObject(ObjectClass.ACCOUNT, uid, null);
        AssertJUnit.assertNull(obj);
    }
}
