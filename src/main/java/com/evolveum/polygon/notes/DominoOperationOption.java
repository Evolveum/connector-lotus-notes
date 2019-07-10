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

import org.identityconnectors.framework.common.objects.OperationOptionInfo;
import org.identityconnectors.framework.common.objects.OperationOptionInfoBuilder;

public enum DominoOperationOption {

    SYNCH_INTERNET_PASSWORD(OperationOptionInfoBuilder.build("SynchInternetPassword", Boolean.class)),
    MAIL_OWNER_ACCESS(OperationOptionInfoBuilder.build("MailOwnerAccess", Integer.class)),
    MAIL_FILE_ACTION(OperationOptionInfoBuilder.build("MailFileAction", Integer.class)),
    DELETE_WINDOWS_USER(OperationOptionInfoBuilder.build("DeleteWindowsUser", Boolean.class));

    private OperationOptionInfo info;

    private DominoOperationOption(OperationOptionInfo info) {
        this.info = info;
    }

    public OperationOptionInfo getInfo() {
        return info;
    }

    public String getName() {
        return info.getName();
    }
}
