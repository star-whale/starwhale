/*
 * Copyright 2022.1-2022
 * StarWhale.ai All right reserved. This software is the confidential and proprietary information of
 * StarWhale.ai ("Confidential Information"). You shall not disclose such Confidential Information and shall use it only
 * in accordance with the terms of the license agreement you entered into with StarWhale.com.
 */

package ai.starwhale.mlops.api.protocol.agent;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import io.swagger.v3.oas.annotations.media.Schema;
import java.io.Serializable;
import lombok.Builder;
import lombok.Data;
import org.aspectj.weaver.loadtime.Agent;
import org.checkerframework.checker.units.qual.A;
import org.springframework.validation.annotation.Validated;

@Data
@Builder
@Validated
@Schema(description = "Agent object", title = "Agent")
public class AgentVO implements Serializable {

    private String id;

    private String ip;

    private Long connectedTime;

    private StatusEnum status;

    private String version;

    public static AgentVO empty() {
        return new AgentVO("", "", -1L, StatusEnum.OFFLINE, "");
    }

    /**
     * Gets or Sets status
     */
    public enum StatusEnum {
        ACTIVE("active"),

        OFFLINE("offline");

        private final String value;

        StatusEnum(String value) {
            this.value = value;
        }

        @Override
        @JsonValue
        public String toString() {
            return String.valueOf(value);
        }

        @JsonCreator
        public static StatusEnum fromValue(String text) {
            for (StatusEnum b : StatusEnum.values()) {
                if (String.valueOf(b.value).equals(text)) {
                    return b;
                }
            }
            return null;
        }
    }

}
