/*
 * Copyright 2022.1-2022
 * StarWhale.ai All right reserved. This software is the confidential and proprietary information of
 * StarWhale.ai ("Confidential Information"). You shall not disclose such Confidential Information and shall use it only
 * in accordance with the terms of the license agreement you entered into with StarWhale.ai.
 */

package ai.starwhale.mlops.domain.storage;

import java.text.SimpleDateFormat;
import java.util.Date;

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
     * %s1 = prefix
     * %s2 = data
     * %s3 = id
     */
    static final String STORAGE_PATH_FORMATTER_RESULT_COLLECTOR = "%s/resultMetrics/%s/%s";

    /**
     * where task result is stored
     * %s1 = prefix
     * %s2 = date
     * %s3 = jobUUID
     * %s4 = taskUUID
     */
    static final String STORAGE_PATH_FORMATTER_RESULT = "%s/result/%s/%s/%s";

    /**
     * where swds is stored
     * %s1 = prefix
     * %s2 = date
     * %s3 = swds id
     * %s4 = swds version id
     */
    static final String STORAGE_PATH_FORMATTER_SWDS = "%s/swds/%s/%s/%s";

    /**
     * where swmp is stored
     * %s1 = prefix
     * %s2 = date
     * %s3 = swmp id
     * %s4 = swmp version id
     */
    static final String STORAGE_PATH_FORMATTER_SWMP = "%s/swmp/%s/%s/%s";

    public StoragePathCoordinator(String systemStoragePathPrefix){
        this.systemStoragePathPrefix = systemStoragePathPrefix;
        this.prefix = String.format(STORAGE_PATH_FORMATTER_PREFIX,systemStoragePathPrefix,SYS_NAME);
    }

    /**
     * only used before path is persisted
     * @param jobUUId
     * @return consistency of path is not guaranteed among multiple method calls
     */
    public String generateResultMetricsPath(String jobUUId){
        return String.format(STORAGE_PATH_FORMATTER_RESULT_COLLECTOR,prefix,currentDate(),jobUUId);
    }

    /**
     * only used before path is persisted
     * @param jobUUId
     * @param taskUUId
     * @return consistency of path is not guaranteed among multiple method calls
     */
    public String generateTaskResultPath(String jobUUId,String taskUUId){
        return String.format(STORAGE_PATH_FORMATTER_RESULT,prefix,currentDate(),jobUUId,taskUUId);
    }

    /**
     * only used before path is persisted
     * @param swdsName
     * @param swdsVersion
     * @return consistency of path is not guaranteed among multiple method calls
     */
    public String generateSwdsPath(String swdsName,String swdsVersion){
        return String.format(STORAGE_PATH_FORMATTER_SWDS,prefix,currentDate(),swdsName,swdsVersion);
    }

    /**
     * only used before path is persisted
     * @param swmpName
     * @param swmpVersion
     * @return consistency of path is not guaranteed among multiple method calls
     */
    public String generateSwmpPath(String swmpName,String swmpVersion){
        return String.format(STORAGE_PATH_FORMATTER_SWMP,prefix,currentDate(),swmpName,swmpVersion);
    }

    static final String FORMAT_DATE="yyyyMMdd";
    String currentDate(){
        SimpleDateFormat simpleDateFormat = new SimpleDateFormat(FORMAT_DATE);
        return simpleDateFormat.format(new Date());
    }

}
