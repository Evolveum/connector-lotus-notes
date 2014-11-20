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

import lotus.domino.NotesException;
import org.apache.commons.lang.StringUtils;
import org.identityconnectors.common.logging.Log;
import org.identityconnectors.framework.common.objects.Attribute;
import org.identityconnectors.framework.common.objects.Name;
import org.identityconnectors.framework.common.objects.ObjectClass;
import org.identityconnectors.framework.common.objects.Uid;
import org.identityconnectors.framework.common.objects.filter.*;

import java.util.Collection;
import java.util.Iterator;

import static com.evolveum.openicf.lotus.util.DominoUtils.*;

public class DominoFilterTranslator extends AbstractFilterTranslator<String> {

    private static final Log LOG = Log.getLog(DominoFilterTranslator.class);

    private DominoConnection connection;
    private DominoConfiguration config;
    private ObjectClass oclass;

    public DominoFilterTranslator(DominoConnection connection, DominoConfiguration config, ObjectClass oclass) {
        this.connection = connection;
        this.config = config;
        this.oclass = oclass;
    }

    @Override
    protected String createAndExpression(String leftExpression, String rightExpression) {
        return createBinaryExpression(leftExpression, rightExpression, "&");
    }

    @Override
    protected String createOrExpression(String leftExpression, String rightExpression) {
        return createBinaryExpression(leftExpression, rightExpression, "|");
    }

    @Override
    protected String createGreaterThanExpression(GreaterThanFilter filter, boolean not) {
        String operator = not ? "<=" : ">";
        return createComparingExpression(getName(filter.getAttribute()), getValue(filter.getAttribute()), operator);
    }

    @Override
    protected String createGreaterThanOrEqualExpression(GreaterThanOrEqualFilter filter, boolean not) {
        String operator = not ? "<" : ">=";
        return createComparingExpression(getName(filter.getAttribute()), getValue(filter.getAttribute()), operator);
    }

    @Override
    protected String createLessThanExpression(LessThanFilter filter, boolean not) {
        String operator = not ? ">=" : "<";
        return createComparingExpression(getName(filter.getAttribute()), getValue(filter.getAttribute()), operator);
    }

    @Override
    protected String createLessThanOrEqualExpression(LessThanOrEqualFilter filter, boolean not) {
        String operator = not ? ">" : "<=";
        return createComparingExpression(getName(filter.getAttribute()), getValue(filter.getAttribute()), operator);
    }

    @Override
    protected String createContainsExpression(ContainsFilter filter, boolean not) {
        String operator = not ? "!" : "";

        String name = getName(filter.getAttribute());
        String value = escape(filter.getValue());

        return createContains(name, value, operator);
    }

    @Override
    protected String createContainsAllValuesExpression(ContainsAllValuesFilter filter, boolean not) {
        String operator = not ? "!" : "";

        String name = getName(filter.getAttribute());
        String value = getValue(filter.getAttribute());

        return createContains(name, value, operator);
    }

    private String createContains(String name, String value, String operator) {
        if (DominoAccountAttribute.SHORT_NAME.getName().equals(name) || useCaseSensitive(name)) {
            LOG.ok("Using case sensitive because attribute is {0}: {1}",
                    DominoAccountAttribute.SHORT_NAME.getName(),
                    DominoAccountAttribute.SHORT_NAME.getName().equals(name));
            return StringUtils.join(new Object[]{operator, "@Contains(", name, "; ", value, ")"});
        }

        String lower = value != null ? value.toLowerCase() : "";
        return StringUtils.join(new Object[]{operator, "@Contains(@LowerCase(", name, "); ", lower, ")"});
    }

    @Override
    protected String createEndsWithExpression(EndsWithFilter filter, boolean not) {
        String notValue = not ? "!" : "";

        String name = getName(filter.getAttribute());
        String value = escape(filter.getValue());
        if (useCaseSensitive(name)) {
            return StringUtils.join(new Object[]{notValue, "@Ends(", name, "; ", value, ")"});
        }

        String lower = value != null ? value.toLowerCase() : "";
        return StringUtils.join(new Object[]{notValue, "@Ends(@LowerCase(", name, "); ", lower, ")"});
    }

    @Override
    protected String createEqualsExpression(EqualsFilter filter, boolean not) {
        String operator = not ? "!=" : "=";

        String name = getName(filter.getAttribute());
        String value = getValue(filter.getAttribute());
        if (useCaseSensitive(name)) {
            return StringUtils.join(new String[]{"(", name, operator, value, ")"});
        }

        String lower = value != null ? value.toLowerCase() : "";
        return StringUtils.join(new Object[]{"(@LowerCase(", name, ')', operator, lower, ')'});
    }

    @Override
    protected String createStartsWithExpression(StartsWithFilter filter, boolean not) {
        String notValue = not ? "!" : "";

        String name = getName(filter.getAttribute());
        String value = escape(filter.getValue());
        if (useCaseSensitive(name)) {
            return StringUtils.join(new Object[]{notValue, "@Begins(", name, "; ", value, ")"});
        }

        String lower = value != null ? value.toLowerCase() : "";
        return StringUtils.join(new Object[]{notValue, "@Begins(@LowerCase(", name, "); ", lower, ")"});
    }

    private boolean useCaseSensitive(String attrName) {
        boolean caseSensitive = !config.getUseCaseInsensitiveSearch() || DominoConstants.NOTE_ID.equals(attrName);
        return caseSensitive;
    }

    private String createComparingExpression(String name, String value, String operator) {
        return StringUtils.join(new Object[]{name, ' ', operator, ' ', escape(value)});
    }

    private String getName(Attribute attribute) {
        String name = attribute.getName();

        if (ObjectClass.GROUP.equals(oclass) && (Uid.NAME.equals(name) || Name.NAME.equals(name))) {
            return DominoGroupAttribute.LIST_NAME.getName();
        } else if (Name.NAME.equals(name)) {
            return DominoAccountAttribute.FULL_NAME.getName();
        } else if (Uid.NAME.equals(name)) {
            return "NoteID";
        }

        return name;
    }

    private String getValue(Attribute attribute) {
        return getValue(attribute.getName(), attribute.getValue());
    }

    private String getValue(String name, Object value) {
        String strVal;
        if (value instanceof Collection) {
            strVal = ((Collection) value).iterator().next().toString();
        } else {
            strVal = value.toString();
        }

        if (ObjectClass.ACCOUNT.equals(oclass)) {
            if (Name.NAME.equals(name)) {
                //create cannonical name for account, also escape it
                try {
                    return escape(getCanonical(connection, strVal));
                } catch (NotesException ex) {
                    handleException(ex, "Couldn't create canonical name for value '" + strVal + "'", LOG);
                }
            } else if (Uid.NAME.equals(name)) {
                //get proper guid and escape it
                return escape(getGuid(strVal));
            }
        } else if (ObjectClass.GROUP.equals(oclass)) {
            if (Name.NAME.equals(name) || Uid.NAME.equals(name)
                    || DominoGroupAttribute.LIST_NAME.getName().equals(name)) {
                //handle group name, escape it
                return escape(getGroupDisplayName(strVal));
            }
        }

        if (value instanceof Collection) {
            StringBuilder sb = new StringBuilder();
            Iterator iterator = ((Collection) value).iterator();
            while (iterator.hasNext()) {
                sb.append(getValue(name, iterator.next()));
                if (iterator.hasNext()) {
                    sb.append(':');
                }
            }
            return sb.toString();
        }

        if (value instanceof Number) {
            return value.toString();
        }

        return escape(value.toString());
    }

    private String escape(String value) {
        return StringUtils.join(new Object[]{'\"', value.replaceAll("\"", "\\\""), "\""});
    }

    private String createBinaryExpression(String leftExpression, String rightExpression, String operator) {
        if (leftExpression == null) {
            return rightExpression;
        }

        if (rightExpression == null) {
            return leftExpression;
        }

        return StringUtils.join(new Object[]{'(', leftExpression, ") ", operator, " (", rightExpression, ')'});
    }
}
