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
import org.testng.annotations.Test;

import java.util.HashSet;
import java.util.Set;

public class CreateOpTest extends BaseDominoTest {

    @Test
    public void createUser() throws Exception {
        Set<Attribute> attrs = createAttrsForJohnDoe(302);
        ConnectorFacade connector = getConnectorFacade();
        Uid uid = connector.create(ObjectClass.ACCOUNT, attrs, null);

        ConnectorObject con = connector.getObject(ObjectClass.ACCOUNT, uid, null);
        System.out.println(con);
    }

    public static Set<Attribute> createAttrsForJohnDoe(int index) {
        Set<Attribute> attrs = new HashSet<Attribute>();
        attrs.add(new Name("CN=John Doe" + index + "/OU=1234/O=EXAMPLE/C=COM"));
        attrs.add(createAttribute(DominoAccountAttribute.FIRST_NAME, "John"));
        attrs.add(createAttribute(DominoAccountAttribute.LAST_NAME, "Doe" + index));
        attrs.add(createAttribute(DominoAccountAttribute.INTERNET_ADDRESS, "John.Doe" + index + "@example.com"));
        attrs.add(createAttribute(DominoAccountAttribute.MAIL_FILE, "mail\\vDoe" + index));
        attrs.add(createAttribute(DominoAccountAttribute.DEPARTMENT, "Some org. unit"));
        attrs.add(createAttribute(DominoAccountAttribute.COMPANY_NAME, "Bratislava"));
        attrs.add(createAttribute(DominoAccountAttribute.JOB_TITLE, "Pizza man"));
        attrs.add(createAttribute(DominoAccountAttribute.SAMETIME_SERVER, "CN=server1/O=example/C=com"));
        attrs.add(createAttribute(DominoAccountAttribute.LOCATION, "Bratislava"));
        attrs.add(createAttribute(DominoAccountAttribute.OFFICE_FAX_PHONE_NUMBER, "9090909"));
        attrs.add(createAttribute(DominoAccountAttribute.CELL_PHONE_NUMBER, "903112233"));
        attrs.add(createAttribute(DominoAccountAttribute.OFFICE_CITY, "Bratislava"));
        attrs.add(createAttribute(DominoAccountAttribute.OFFICE_STATE, "Bratislava"));
        attrs.add(createAttribute(DominoAccountAttribute.OFFICE_COUNTRY, "Slovakia"));
        attrs.add(createAttribute(DominoAccountAttribute.OFFICE_NUMBER, "421211221122"));
        attrs.add(createAttribute(DominoAccountAttribute.MAIL_SERVER, "CN=server1/O=example/C=com"));
        attrs.add(createAttribute(DominoAccountAttribute.MAIL_DOMAIN, "EXAMPLE"));
        attrs.add(createAttribute(DominoAccountAttribute.LOCAL_ADMIN, "LocalDomainAdmins"));
        attrs.add(createAttribute(DominoAccountAttribute.SHORT_NAME, "vDoe" + index, "sn" + index));

        attrs.add(createAttribute(DominoAccountAttribute.CERTIFIER_ORG_HIERARCHY, "/example/COM"));
        attrs.add(createAttribute(DominoAccountAttribute.ORG_UNIT, "1234"));

        attrs.add(createAttribute(DominoAccountAttribute.ID_FILE, "./target/test-id-folder/vdoe" + index + ".id"));
        attrs.add(AttributeBuilder.buildPassword("asdf.123".toCharArray()));
        attrs.add(AttributeBuilder.buildEnabled(true));

        return attrs;
    }

    public static Attribute createAttribute(DominoAttribute attr, Object... values) {
        return AttributeBuilder.build(attr.getName(), values);
    }
}
