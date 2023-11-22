/*
 * Copyright 2022 Starwhale, Inc. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package ai.starwhale.mlops.exception;

public class SwValidationException extends StarwhaleException {

    static final String PREFIX_CODE = "INVALID REQUEST: ";

    static final String PREFIX_TIP = "invalid request on subject ";

    private final String code;
    private final String tip;

    public SwValidationException(ValidSubject validSubject) {
        super(PREFIX_TIP + validSubject.tipSubject);
        this.code = PREFIX_CODE + validSubject.tipSubject;
        this.tip = PREFIX_TIP + validSubject.tipSubject;
    }

    public SwValidationException(ValidSubject validSubject, String tip) {
        super(PREFIX_TIP + validSubject.tipSubject + "\n" + tip);
        this.code = PREFIX_CODE + validSubject.tipSubject;
        this.tip = PREFIX_TIP + validSubject.tipSubject + "\n" + tip;
    }

    public SwValidationException(ValidSubject validSubject, String tip, Throwable cause) {
        super(PREFIX_TIP + validSubject.tipSubject + "\n" + cause.getMessage() + "\n" + tip, cause);
        this.code = PREFIX_CODE + validSubject.tipSubject;
        this.tip = PREFIX_TIP + validSubject.tipSubject + "\n" + cause.getMessage() + "\n" + tip;
    }

    @Override
    public String getCode() {
        return this.code;
    }

    @Override
    public String getTip() {
        return this.tip;
    }

    public enum ValidSubject {
        JOB("001", "JOB"),
        TASK("002", "TASK"),
        USER("003", "USER"),
        NODE("004", "NODE"),
        DATASET("005", "Starwhale DataSet"),
        MODEL("006", "Starwhale Model"),
        PROJECT("007", "PROJECT"),
        RUNTIME("008", "Starwhale Runtime"),
        DATASTORE("009", "Starwhale Internal DataStore"),
        RESOURCE_POOL("010", "Resource Pool"),
        SETTING("011", "System Setting"),
        TRASH("012", "TRASH"),
        OBJECT_STORE("013", "Object Store"),
        PLUGIN("014", "Plugin"),
        ONLINE_EVAL("015", "Online Eval"),
        UPGRADE("016", "Upgrade"),
        TAG("017", "Resource Tag"),
        REPORT("018", "Report"),
        FINE_TUNE("019", "Fine Tune"),
        EVALUATION("020", "Evaluation");
        final String code;
        final String tipSubject;

        ValidSubject(String code, String tipSubject) {
            this.code = code;
            this.tipSubject = tipSubject;
        }

        public String getCode() {
            return code;
        }

        public String getTipSubject() {
            return tipSubject;
        }
    }
}
