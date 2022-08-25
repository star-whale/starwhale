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

package ai.starwhale.mlops.domain.swds.datastore;

import org.springframework.stereotype.Component;

/**
 * data store helper for data set
 */
@Component
public class DataStoreTableNameHelper {

    final int VERSION_PREFIX_CNT = 2;

    public static final String FORMATTER_TABLE_NAME_DATASET ="project/%s/dataset/%s/%s/%s/meta";

    public static final String FORMATTER_TABLE_NAME_EVAL_RESULTS ="project/%s/eval/%s/results";

    public static final String FORMATTER_TABLE_NAME_EVAL_SUMMARY ="project/%s/eval/summary";

    public String tableNameOfDataset(String project,String name,String version){
        return String.format(FORMATTER_TABLE_NAME_DATASET,project,name,version.substring(0,VERSION_PREFIX_CNT),version);
    }

}
