/*
 * Copyright 2022.1-2022
 * StarWhale.ai All right reserved. This software is the confidential and proprietary information of
 * StarWhale.ai ("Confidential Information"). You shall not disclose such Confidential Information and shall use it only
 * in accordance with the terms of the license agreement you entered into with StarWhale.ai.
 */

package ai.starwhale.mlops.domain.storage;

/**
 * coordinator all paths in this system
 */
public class StoragePathCoordinator {

    final static String SYS_NAME="controller";

    String systemStoragePathPrefix;

    String prefix;

    /**
     * controller root path
     */
    static final String STORAGE_PATH_FORMATTER_PREFIX = "%s/%s";

    /**
     * where collected result metrics is stored
     * %s1 = prefix %s2 = id
     */
    static final String STORAGE_PATH_FORMATTER_RESULT_COLLECTOR = "%s/resultMetrics/%s";

    /**
     * where collected result metrics is stored
     * %s1 = prefix %s2 = id
     */
    static final String STORAGE_PATH_FORMATTER_RESULT_METRICS = "%s/resultMetrics";

    /**
     * where task result is stored
     * %s1 = prefix
     * %s2 = jobUUID
     * %s3 = taskUUID
     */
    static final String STORAGE_PATH_FORMATTER_RESULT = "%s/result/%s/%s";

    /**
     * where swds is stored
     * %s1 = prefix
     * %s2 = swds id
     * %s3 = swds version id
     */
    static final String STORAGE_PATH_FORMATTER_SWDS = "%s/swds/%s/%s";

    /**
     * where swmp is stored
     * %s1 = prefix
     * %s2 = swmp id
     * %s3 = swmp version id
     */
    static final String STORAGE_PATH_FORMATTER_SWMP = "%s/swmp/%s/%s";

    public StoragePathCoordinator(String systemStoragePathPrefix){
        this.systemStoragePathPrefix = systemStoragePathPrefix;
        //todo(renyanda) add date to path
        this.prefix = String.format(STORAGE_PATH_FORMATTER_PREFIX,systemStoragePathPrefix,SYS_NAME);
    }

    public String resultMetricsPath(String metricsId){
        return String.format(STORAGE_PATH_FORMATTER_RESULT_COLLECTOR,prefix,metricsId);
    }

    public String resultMetricsPath(){
        return String.format(STORAGE_PATH_FORMATTER_RESULT_METRICS,prefix);
    }

    public String taskResultPath(String jobId,String taskId){
        return String.format(STORAGE_PATH_FORMATTER_RESULT,prefix,jobId,taskId);
    }

    public String swdsPath(String swdsId,String swdsVersionId){
        return String.format(STORAGE_PATH_FORMATTER_SWDS,prefix,swdsId,swdsVersionId);
    }

    public String swmpPath(String swmpId,String swmpVersionId){
        return String.format(STORAGE_PATH_FORMATTER_SWMP,prefix,swmpId,swmpVersionId);
    }

}
