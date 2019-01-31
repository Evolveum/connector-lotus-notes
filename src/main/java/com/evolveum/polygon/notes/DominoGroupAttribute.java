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

import org.identityconnectors.framework.common.objects.AttributeInfo;

import java.util.*;

import static org.identityconnectors.framework.common.objects.AttributeInfo.Flags.*;

public enum DominoGroupAttribute implements DominoAttribute {

    COMMENTS("Comments"),
    DISPLAY_NAME("DisplayName", String.class, NOT_UPDATEABLE, NOT_CREATABLE),
    GROUP_NAME("GroupName"),
    GROUP_TITLE("GroupTitle"),
    /**
     * GroupType attribute values in lotus domino:
     * <p/>
     * 0 - Multi-purpose
     * 1 - Mail only
     * 2 - Access Control List Only
     * 3 - Deny List only
     * 4 - Servers only
     */
    GROUP_TYPE("GroupType", Integer.class),
    LAST_MODIFIED("LastModified", Long.class, NOT_UPDATEABLE, NOT_CREATABLE),
    LIST_CATEGORY("ListCategory"),
    LIST_DESCRIPTION("ListDescription"),
    LIST_NAME("ListName"),
    MEMBER_GROUPS("MemberGroups", String.class, NOT_RETURNED_BY_DEFAULT, MULTIVALUED),
    MEMBER_PEOPLE("MemberPeople", String.class, NOT_RETURNED_BY_DEFAULT, MULTIVALUED),
    MEMBERS("Members", String.class, MULTIVALUED),
    OBJECT_GUID("objectGUID", String.class, NOT_UPDATEABLE, NOT_CREATABLE);

    private static final Map<String, DominoGroupAttribute> ATTRIBUTE_MAP = new HashMap<String, DominoGroupAttribute>();

    static {
        for (DominoGroupAttribute attr : DominoGroupAttribute.values()) {
            ATTRIBUTE_MAP.put(attr.getName(), attr);
        }
    }

    private String name;
    private Class type;
    private Set<AttributeInfo.Flags> flags;

    private AttributeInfo attribute;

    private DominoGroupAttribute(AttributeInfo attribute) {
        this.attribute = attribute;
    }

    private DominoGroupAttribute(String name) {
        this(name, String.class);
    }

    private DominoGroupAttribute(String name, Class type, AttributeInfo.Flags... flags) {
        this.name = name;
        this.type = type;

        Set<AttributeInfo.Flags> set = new HashSet<AttributeInfo.Flags>();
        for (AttributeInfo.Flags flag : flags) {
            set.add(flag);
        }
        this.flags = Collections.unmodifiableSet(set);
    }

    public Set<AttributeInfo.Flags> getFlags() {
        if (attribute != null) {
            return attribute.getFlags();
        }
        return flags;
    }

    public String getName() {
        if (attribute != null) {
            return attribute.getName();
        }
        return name;
    }

    public Class getType() {
        if (attribute != null) {
            return attribute.getType();
        }
        return type;
    }

    public boolean isOperational() {
        return attribute != null;
    }

    public AttributeInfo getAttribute() {
        return attribute;
    }

    public static DominoGroupAttribute getAttribute(String name) {
        return ATTRIBUTE_MAP.get(name);
    }
}
