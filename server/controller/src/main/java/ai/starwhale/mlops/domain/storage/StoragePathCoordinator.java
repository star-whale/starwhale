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

package ai.starwhale.mlops.domain.storage;

import ai.starwhale.mlops.exception.SwValidationException;
import ai.starwhale.mlops.exception.SwValidationException.ValidSubject;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

/**
 * coordinator all paths in this system
 */
@Slf4j
public class StoragePathCoordinator {

    static final String SYS_NAME = "controller";

    @Getter
    private final String swdsPathNamedFormatter;

    String systemStoragePathPrefix;

    String prefix;

    /**
     * controller root path
     */
    static final String STORAGE_PATH_FORMATTER_PREFIX = "%s/%s";

    /**
     * where collected result metrics is stored %s1 = prefix %s2 = data %s3 = id
     */
    static final String STORAGE_PATH_FORMATTER_RESULT_COLLECTOR = "%s/resultMetrics/%s/%s";

    /**
     * where task result is stored %s1 = prefix %s2 = date %s3 = jobUuid %s4 = taskUuid
     */
    static final String STORAGE_PATH_FORMATTER_RESULT = "%s/result/%s/%s/%s";

    /**
     * where swds is stored %s1 = prefix %s2 = swds name %s3 = swds version
     */
    static final String STORAGE_PATH_FORMATTER_SWDS = "%s/project/%s/dataset/%s/version/%s";

    static final String STORAGE_PATH_FORMATTER_SWDS_NAMED =
            "%s/project/{projectName}/dataset/{datasetName}/version/{versionName}";

    /**
     * where swmp is stored %s1 = prefix %s2 = swmp name %s3 = swmp version
     */
    static final String STORAGE_PATH_FORMATTER_SWMP = "%s/project/%s/model/%s/version/%s";

    static final String STORAGE_PATH_FORMATTER_SWRT = "%s/project/%s/runtime/%s/version/%s";

    public StoragePathCoordinator(String systemStoragePathPrefix) {
        this.systemStoragePathPrefix = systemStoragePathPrefix;
        this.prefix = String.format(STORAGE_PATH_FORMATTER_PREFIX, systemStoragePathPrefix,
                SYS_NAME);
        this.swdsPathNamedFormatter = String.format(STORAGE_PATH_FORMATTER_SWDS_NAMED, prefix);
    }

    /**
     * @return consistency of path is guaranteed among multiple method calls
     */
    public String generateResultMetricsPath(String jobUuid) {
        checkKeyWord(jobUuid, ValidSubject.JOB);
        return String.format(STORAGE_PATH_FORMATTER_RESULT_COLLECTOR, prefix,
                firstTwoLetter(jobUuid), jobUuid);
    }

    /**
     * @return consistency of path is guaranteed among multiple method calls
     */
    public String generateTaskResultPath(String jobUuid, String taskUuid) {
        checkKeyWord(jobUuid, ValidSubject.JOB);
        return String.format(STORAGE_PATH_FORMATTER_RESULT, prefix, firstTwoLetter(jobUuid),
                jobUuid, taskUuid);
    }

    /**
     * @return consistency of path is guaranteed among multiple method calls
     */
    public String generateSwdsPath(String project, String swdsName, String swdsVersion) {
        checkKeyWord(swdsVersion, ValidSubject.SWDS);
        return String.format(STORAGE_PATH_FORMATTER_SWDS, prefix, project, swdsName, swdsVersion);
    }

    /**
     * @return consistency of path is guaranteed among multiple method calls
     */
    public String generateSwmpPath(String projectName, String swmpName, String swmpVersion) {
        checkKeyWord(swmpVersion, ValidSubject.SWMP);
        return String.format(STORAGE_PATH_FORMATTER_SWMP, prefix, projectName, swmpName,
                swmpVersion);
    }

    public String generateRuntimePath(String projectName, String runtimeName,
            String runtimeVersion) {
        checkKeyWord(runtimeVersion, ValidSubject.RUNTIME);
        return String.format(STORAGE_PATH_FORMATTER_SWRT, prefix, projectName, runtimeName,
                runtimeVersion);
    }

    private void checkKeyWord(String kw, ValidSubject validSubject) {
        if (null == kw || kw.isBlank()) {
            throw new SwValidationException(validSubject).tip(
                    "allocated storage key word can't be empty");
        }
        if (kw.length() < 2) {
            throw new SwValidationException(validSubject).tip(
                    "allocated storage key word too short");
        }
    }

    private String firstTwoLetter(String s) {
        return s.substring(0, 2);
    }

}
