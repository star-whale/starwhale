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

package ai.starwhale.mlops.domain.job.step.task.bo;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class ResultPath {

    String root;

    /**
     * the dir that contains execution result , no hierarchy ,flat
     */
    String resultDir;

    /**
     * the dir of logs , no hierarchy ,flat
     */
    String logDir;

    static final String DIR_RESULT = "/result";

    static final String DIR_LOG = "/logs";

    public ResultPath(String rootPath) {
        this.root = rootPath;
        this.resultDir = DIR_RESULT;
        this.logDir = DIR_LOG;
    }

    public String resultDir() {
        return this.root + resultDir;
    }

    public String logDir() {
        return this.root + logDir;
    }
}
