/*
 * Copyright 2022.1-2022
 *  starwhale.ai All right reserved. This software is the confidential and proprietary information of
 *  starwhale.ai ("Confidential Information"). You shall not disclose such Confidential Information and shall use it only
 * in accordance with the terms of the license agreement you entered into with  starwhale.ai.
 */

package ai.starwhale.mlops.agent.exception;

/**
 * error of agent
 */
public enum ErrorCode {
    allocateError,
    containerError,
    uploadError,
    downloadError;

    public AgentException asException(String msg) {
        return new AgentException(String.format("%s: %s", this.name(), msg));
    }
}
