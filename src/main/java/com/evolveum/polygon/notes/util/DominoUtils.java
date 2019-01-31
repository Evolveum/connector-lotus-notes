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

import com.evolveum.polygon.notes.*;
import lotus.domino.Base;
import lotus.domino.Name;
import lotus.domino.NotesException;
import org.apache.commons.lang.StringUtils;
import org.identityconnectors.common.StringUtil;
import org.identityconnectors.common.logging.Log;
import org.identityconnectors.common.security.GuardedString;
import org.identityconnectors.framework.common.exceptions.ConnectorException;
import org.identityconnectors.framework.common.objects.*;

import java.util.*;

public final class DominoUtils {

    public static final Set<String> ACCOUNT_ATTRIBUTE_NAMES;
    public static final Set<String> GROUP_ATTRIBUTE_NAMES;

    static {
        Set<String> set = getAttributeNames(DominoAccountAttribute.class);
        ACCOUNT_ATTRIBUTE_NAMES = Collections.unmodifiableSet(set);

        set = getAttributeNames(DominoGroupAttribute.class);
        GROUP_ATTRIBUTE_NAMES = Collections.unmodifiableSet(set);
    }

    private DominoUtils() {
    }

    public static String getExceptionMessage(NotesException ex) {
        StringBuilder sb = new StringBuilder();
        sb.append("m: ");
        sb.append(ex.getMessage());
        sb.append(", t: ");
        sb.append(ex.text);

        return sb.toString();
    }

    private static Set<String> getAttributeNames(Class<? extends DominoAttribute> clazz) {
        Set<String> names = new HashSet<String>();

        DominoAttribute[] enums = clazz.getEnumConstants();
        for (DominoAttribute attr : enums) {
            names.add(attr.getName());
        }

        return names;
    }

    public static String getCommon(DominoConnection connection, String value) throws NotesException {
        return connection.getSession().createName(value).getCommon();
    }

    public static String getAbbreviated(DominoConnection connection, String value) throws NotesException {
        return connection.getSession().createName(value).getAbbreviated();
    }

    public static String getCanonical(DominoConnection connection, String value) throws NotesException {
        return connection.getSession().createName(value).getCanonical();
    }

    public static Set<String> createAttributesToGet(ObjectClass oclass, OperationOptions options) {
        Set<String> attributes = oclass.equals(ObjectClass.ACCOUNT)
                ? DominoUtils.ACCOUNT_ATTRIBUTE_NAMES : DominoUtils.GROUP_ATTRIBUTE_NAMES;

        String[] attrsToGet = options != null ? options.getAttributesToGet() : null;
        if (attrsToGet == null || attrsToGet.length == 0) {
            return attributes;
        }

        return new HashSet<String>(Arrays.asList(attrsToGet));
    }

    public static String getFirstValueString(List<Object> values) {
        if (values.isEmpty()) {
            return null;
        }

        Object first = values.get(0);
        return first != null ? first.toString() : null;
    }

    public static String getAttributeValue(Map<String, Attribute> attrs, DominoAttribute attr) {
        return getAttributeValue(attrs, attr, String.class);
    }

    public static <T> T getAttributeValue(Map<String, Attribute> attrs, DominoAttribute attr, Class<T> type) {
        return getAttributeValue(attrs, attr, type, null);
    }

    public static <T> T getAttributeValue(Map<String, Attribute> attrs, DominoAttribute attr,
                                          Class<T> type, T defaultValue) {
        return getAttributeValue(attrs, attr, type, defaultValue, true);
    }

    public static <T> T getAttributeValue(Map<String, Attribute> attrs, DominoAttribute attr,
                                          Class<T> type, T defaultValue, boolean removeFromMap) {
        Attribute attribute = removeFromMap ? attrs.remove(attr.getName()) : attrs.get(attr.getName());
        return getAttributeValue(attribute, type, defaultValue);
    }

    public static <T> T getAttributeValue(Attribute attr, Class<T> type, T defaultValue) {
        if (attr == null) {
            return defaultValue;
        }

        if (String.class.equals(type)) {
            return (T) AttributeUtil.getAsStringValue(attr);
        } else if (Integer.class.equals(type)) {
            return (T) AttributeUtil.getIntegerValue(attr);
        } else if (Long.class.equals(type)) {
            return (T) AttributeUtil.getLongValue(attr);
        } else if (Boolean.class.equals(type)) {
            return (T) AttributeUtil.getBooleanValue(attr);
        } else if (Vector.class.equals(type)) {
            if (attr.getValue() == null) {
                return null;
            }
            return (T) new Vector(attr.getValue());
        } else if (GuardedString.class.equals(type)) {
            return (T) AttributeUtil.getGuardedStringValue(attr);
        }

        throw new IllegalArgumentException("Can't get attribute value, unknown type '" + type + "'.");
    }

    public static String decode(GuardedString guarded) {
        if (guarded == null) {
            return null;
        }

        GuardedStringAccessor accessor = new GuardedStringAccessor();
        guarded.access(accessor);
        return accessor.getString();
    }

    public static void recycleQuietly(Base base) {
        if (base == null) {
            return;
        }

        try {
            base.recycle();
        } catch (NotesException ex) {
        }
    }

    public static String normalizeSpaces(String value) {
        if (value == null) {
            return null;
        }

        return value.replaceAll("^\\s*|\\s(?=\\s)|\\s*$", "");
    }

    public static String createFullName(DominoConnection connection, String firstName, String middleInitial,
                                        String lastName, String orgUnit, String certifierOrgHierarchy)
            throws NotesException {
        String name = join(new String[]{firstName, middleInitial, lastName}, " ");
        String fullName = join(new String[]{name, orgUnit, certifierOrgHierarchy}, "/");

        return getCanonical(connection, fullName);
    }

    public static Integer getDays(Long endDate) {
        if (endDate == null) {
            return null;
        }

        Long days = (endDate - System.currentTimeMillis()) / 86400000L;
        return days.intValue();
    }

    public static <T> T getOperationOptionValue(OperationOptions options, DominoOperationOption option,
                                                T defaultValue) {
        Map<String, Object> opts = options.getOptions();
        if (opts == null || !opts.containsKey(option.getName())) {
            return defaultValue;
        }

        return (T) opts.get(option);
    }

    public static void handleException(NotesException ex, String message, Log log) {
        String exMessage = DominoUtils.getExceptionMessage(ex);
        log.error(ex, "{0}, error id: {1}, reason: {2}", message, ex.id, exMessage);
        throw new ConnectorException(message + ", error id: " + ex.id + ", reason: " + exMessage);
    }

    public static String getGroupDisplayName(String groupName) {
        if (groupName == null) {
            return null;
        }
        String[] names = groupName.split(";");
        return names[0];
    }

    public static String getGuid(String guid) {
        int start = guid.indexOf("<GUID=");
        int finish = guid.indexOf(">");
        if (start > -1 && finish > -1) {
            return guid.substring(start + 6, finish);
        }
        return guid;
    }

    public static boolean isAttrToGet(Collection attrs, String attrName) {
        return attrs.contains(attrName);
    }

    public static boolean isAttrToGet(Collection attrs, DominoAttribute attr) {
        return isAttrToGet(attrs, attr.getName());
    }

    public static String getOrgFromName(DominoConnection connection, String name) throws NotesException {
        if (name == null) {
            return null;
        }

        Name dominoName = connection.getSession().createName(name);
        String org = dominoName.getOrganization();
        if (StringUtil.isNotBlank(org)) {
            org = StringUtils.join(new String[]{"/", org});
        }
        return org;
    }

    public static String join(String[] objects, String delimiter){
        List<Object> filtered = new ArrayList<Object>();
        for (String obj : objects) {
            if (StringUtils.isEmpty(obj)) {
                continue;
            }

            filtered.add(obj);
        }
        return StringUtils.join(filtered, delimiter);
    }

    public static String getOrgUnit(DominoConnection connection, String fullName) throws NotesException {
        Name name = connection.getSession().createName(fullName);

        return join(new String[]{name.getOrgUnit1(), name.getOrgUnit2(), name.getOrgUnit3(),
                name.getOrgUnit4()}, "\\");
    }

    public static Attribute build(DominoAttribute attr, Object... values) {
        return AttributeBuilder.build(attr.getName(), values);
    }

    public static Object getDominoValues(Attribute attr) {
        if (attr == null || attr.getValue() == null) {
            return null;
        }

        List<Object> values = attr.getValue();
        if (values.size() == 1) {
            return values.get(0);
        }

        return new Vector(values);
    }
}
