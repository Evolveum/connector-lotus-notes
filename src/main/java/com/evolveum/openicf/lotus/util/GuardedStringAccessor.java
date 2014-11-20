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

package com.evolveum.openicf.lotus.util;

import org.identityconnectors.common.security.GuardedString;

import java.util.Arrays;

public class GuardedStringAccessor implements GuardedString.Accessor {

    private char[] chars;

    public void access(char[] clearChars) {
        if (clearChars == null) {
            return;
        }

        chars = new char[clearChars.length];
        System.arraycopy(clearChars, 0, chars, 0, chars.length);
    }

    public String getString() {
        if (chars == null || chars.length == 0) {
            return null;
        }

        return new String(chars);
    }

    public void clear() {
        if (chars == null) {
            return;
        }

        Arrays.fill(chars, '\u0000');
        chars = null;
    }

    public static String getString(GuardedString guarded) {
        if (guarded == null) {
            return null;
        }

        GuardedStringAccessor accessor = new GuardedStringAccessor();
        guarded.access(accessor);
        String value = accessor.getString();
        accessor.clear();

        return value;
    }
}
