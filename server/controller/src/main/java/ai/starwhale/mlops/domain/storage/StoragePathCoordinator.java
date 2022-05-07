/**
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

import ai.starwhale.mlops.exception.SWValidationException;
import ai.starwhale.mlops.exception.SWValidationException.ValidSubject;
import java.text.SimpleDateFormat;
import java.util.Date;
import lombok.extern.slf4j.Slf4j;

/**
 * coordinator all paths in this system
 */
@Slf4j
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
     * %s2 = swds name
     * %s3 = swds version
     */
    static final String STORAGE_PATH_FORMATTER_SWDS = "%s/swds/%s/%s";

    /**
     * where swmp is stored
     * %s1 = prefix
     * %s2 = swmp name
     * %s3 = swmp version
     */
    static final String STORAGE_PATH_FORMATTER_SWMP = "%s/swmp/%s/%s";

    public StoragePathCoordinator(String systemStoragePathPrefix){
        this.systemStoragePathPrefix = systemStoragePathPrefix;
        this.prefix = String.format(STORAGE_PATH_FORMATTER_PREFIX,systemStoragePathPrefix,SYS_NAME);
    }

    /**
     * 
     * @param jobUUId
     * @return consistency of path is guaranteed among multiple method calls
     */
    public String generateResultMetricsPath(String jobUUId){
        checkKeyWord(jobUUId,ValidSubject.JOB);
        return String.format(STORAGE_PATH_FORMATTER_RESULT_COLLECTOR,prefix,firstTwoLetter(jobUUId),jobUUId);
    }

    /**
     * 
     * @param jobUUId
     * @param taskUUId
     * @return consistency of path is guaranteed among multiple method calls
     */
    public String generateTaskResultPath(String jobUUId,String taskUUId){
        checkKeyWord(jobUUId,ValidSubject.JOB);
        return String.format(STORAGE_PATH_FORMATTER_RESULT,prefix,firstTwoLetter(jobUUId),jobUUId,taskUUId);
    }

    /**
     * 
     * @param swdsName
     * @param swdsVersion
     * @return consistency of path is guaranteed among multiple method calls
     */
    public String generateSwdsPath(String swdsName,String swdsVersion){
        checkKeyWord(swdsVersion,ValidSubject.SWDS);
        return String.format(STORAGE_PATH_FORMATTER_SWDS,prefix,swdsName,swdsVersion);
    }

    /**
     * 
     * @param swmpName
     * @param swmpVersion
     * @return consistency of path is guaranteed among multiple method calls
     */
    public String generateSwmpPath(String swmpName,String swmpVersion){
        checkKeyWord(swmpVersion,ValidSubject.SWMP);
        return String.format(STORAGE_PATH_FORMATTER_SWMP,prefix,swmpName,swmpVersion);
    }

    private void checkKeyWord(String kw, ValidSubject validSubject){
        if(null == kw || kw.isBlank()){
            throw new SWValidationException(validSubject).tip("allocated storage key word can't be empty");
        }
        if(kw.length()<2){
            throw new SWValidationException(validSubject).tip("allocated storage key word too short");
        }
    }

    private String firstTwoLetter(String s){
        return s.substring(0,2);
    }

}
