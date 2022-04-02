/*
 * Copyright 2022.1-2022
 * StarWhale.ai All right reserved. This software is the confidential and proprietary information of
 * StarWhale.ai ("Confidential Information"). You shall not disclose such Confidential Information and shall use it only
 * in accordance with the terms of the license agreement you entered into with StarWhale.com.
 */

package ai.starwhale.mlops.agent.configuration;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Data
@ConfigurationProperties(prefix = "sw.agent")
public class AgentProperties {

    private Task task;
    private Container container;

    @Data
    public static class Task {

        /**
         * taskInfo dir path,Eg:/var/starwhale/task/info/,
         * and every file of taskInfo will locate at path/{taskId}.taskinfo(format:json)
         */
        private String infoPath;

        /**
         * task running status dir path,Eg:/var/starwhale/task/status/,
         * and every file of taskStatus will locate at path/{taskId}.status(format:txt)
         */
        private String statusPath;

        /**
         * swmp dir path,Eg:/var/starwhale/task/swmp/,
         * and every modelDir of task will locate at path/{taskId}/
         */
        private String swmpPath;

        /**
         * archived taskInfo file path,Eg:/var/starwhale/task/archived/
         */
        private String archivedDirPath;

        /**
         * task result dir path,Eg:/var/starwhale/task/result/,
         * and every task's result file will locate at path/{taskId}/xxxResult
         */
        private String resultPath;

        /**
         * task runtime log dir path,Eg:/var/starwhale/task/log/,
         * and every task's log file will locate at path/{taskId}/xxx.log
         */
        private String logPath;
    }

    @Data
    public static class Container {

        /**
         * docker host:linux-unix:///var/run/docker.sock,windows-npipe:////./pipe/docker_engine
         */
        private String host;
    }
}
