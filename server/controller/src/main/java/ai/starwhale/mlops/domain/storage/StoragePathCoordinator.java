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
import lombok.extern.slf4j.Slf4j;
import org.springframework.util.StringUtils;

/**
 * coordinator all paths in this system
 */
@Slf4j
public class StoragePathCoordinator {

    static final String SYS_NAME = "controller";

    String systemStoragePathPrefix;

    String prefix;

    /**
     * controller root path
     */
    static final String STORAGE_PATH_FORMATTER_PREFIX = "%s/%s";

    public StoragePathCoordinator(String systemStoragePathPrefix) {
        this.systemStoragePathPrefix = systemStoragePathPrefix;
        this.prefix = String.format(STORAGE_PATH_FORMATTER_PREFIX, systemStoragePathPrefix,
                SYS_NAME);
    }

    /**
     * where collected result metrics is stored %s1 = prefix %s2 = data %s3 = id
     */
    static final String STORAGE_PATH_FORMATTER_RESULT_COLLECTOR = "%s/resultMetrics/%s/%s";

    /**
     * @return consistency of path is guaranteed among multiple method calls
     */
    public String allocateResultMetricsPath(String jobUuid) {
        checkKeyWord(jobUuid, ValidSubject.JOB);
        return String.format(STORAGE_PATH_FORMATTER_RESULT_COLLECTOR, prefix,
                firstTwoLetter(jobUuid), jobUuid);
    }

    /**
     * where task result is stored %s1 = prefix %s2 = date %s3 = jobUuid %s4 = taskUuid
     */
    static final String STORAGE_PATH_FORMATTER_RESULT = "%s/result/%s/%s/%s";

    /**
     * @return consistency of path is guaranteed among multiple method calls
     */
    public String allocateTaskResultPath(String jobUuid, String taskUuid) {
        checkKeyWord(jobUuid, ValidSubject.JOB);
        return String.format(STORAGE_PATH_FORMATTER_RESULT, prefix, firstTwoLetter(jobUuid),
                jobUuid, taskUuid);
    }

    /**
     * @return consistency of path is guaranteed among multiple method calls
     */
    public String allocateDatasetVersionPath(Long projectId, String datasetName, String datasetVersion) {
        checkKeyWord(datasetVersion, ValidSubject.DATASET);
        return allocateBundlePath(projectId, BUNDLE_DATASET, datasetName, datasetVersion);
    }

    public String allocateDatasetPath(Long projectId, String datasetName) {
        return String.format(STORAGE_PATH_FORMATTER_DATASET_POOL, prefix, projectId, datasetName);
    }

    /**
     * @return consistency of path is guaranteed among multiple method calls
     */
    public String allocateModelPath(Long projectId, String modelName, String modelVersion) {
        checkKeyWord(modelVersion, ValidSubject.MODEL);
        return allocateBundlePath(projectId, BUNDLE_MODEL, modelName, modelVersion);
    }

    public String allocateCommonModelPoolPath(Long projectId, String signature) {
        return String.format(STORAGE_PATH_FORMATTER_MODEL_POOL,
                prefix, projectId, signature);
    }

    public String allocateRuntimePath(Long projectId, String runtimeName,
            String runtimeVersion) {
        checkKeyWord(runtimeVersion, ValidSubject.RUNTIME);
        return allocateBundlePath(projectId, BUNDLE_RUNTIME, runtimeName, runtimeVersion);
    }

    static final String BUNDLE_DATASET = "dataset";
    static final String BUNDLE_MODEL = "model";
    static final String BUNDLE_RUNTIME = "runtime";
    /**
     * {prefix}/project/{projectName}/{bundleType}/{bundleName}/version/{bundleVersion}
     */
    static final String STORAGE_PATH_FORMATTER_BUNDLE = "%s/project/%s/%s/%s/version/%s";
    static final String STORAGE_PATH_FORMATTER_MODEL_POOL = "%s/project/%s/common-model/%s";

    /**
     * {prefix}/project/{projectId}/common-dataset/{datasetName}
     */
    static final String STORAGE_PATH_FORMATTER_DATASET_POOL = "%s/project/%s/common-dataset/%s";

    /**
     * {prefix}/project/{projectId}/common-eval/{evalVersion}
     */
    static final String STORAGE_PATH_FORMATTER_EVAL_POOL = "%s/project/%s/common-eval/%s";

    public String allocateEvalStoragePath(Long projectId, String evalVersion) {
        return String.format(STORAGE_PATH_FORMATTER_EVAL_POOL, prefix, projectId, evalVersion);
    }

    public String allocateBundlePath(Long projectId, String bundleType, String bundleName,
            String bundleVersion) {
        return String.format(STORAGE_PATH_FORMATTER_BUNDLE, prefix, projectId, bundleType, bundleName,
                bundleVersion);
    }

    static final String PLUGIN_PREFIX = "plugins";
    static final String PANEL_PREFIX = "panel";

    public String allocatePluginPath(String name, String version) {
        var parts = new String[]{prefix, PLUGIN_PREFIX, PANEL_PREFIX, name, version};
        return StringUtils.arrayToDelimitedString(parts, "/");
    }

    private void checkKeyWord(String kw, ValidSubject validSubject) {
        if (null == kw || kw.isBlank()) {
            throw new SwValidationException(validSubject, "allocated storage key word can't be empty");
        }
        if (kw.length() < 2) {
            throw new SwValidationException(validSubject, "allocated storage key word too short");
        }
    }

    private String firstTwoLetter(String s) {
        return s.substring(0, 2);
    }

}
