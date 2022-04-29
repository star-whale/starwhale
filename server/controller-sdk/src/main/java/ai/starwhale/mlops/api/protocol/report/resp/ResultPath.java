/*
 * Copyright 2022.1-2022
 * StarWhale.ai All right reserved. This software is the confidential and proprietary information of
 * StarWhale.ai ("Confidential Information"). You shall not disclose such Confidential Information and shall use it only
 * in accordance with the terms of the license agreement you entered into with StarWhale.ai.
 */

package ai.starwhale.mlops.api.protocol.report.resp;

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

    static final String DIR_RESULT="/result";

    static final String DIR_LOG="/logs";

    public ResultPath(String rootPath){
        this.root = rootPath;
        this.resultDir = DIR_RESULT;
        this.logDir = DIR_LOG;
    }

    public String resultDir() {
        return this.root +  resultDir;
    }

    public String logDir(){
        return this.root +  logDir;
    }
}
