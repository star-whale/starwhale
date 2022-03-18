/*
 * Copyright 2022.1-2022
 * StarWhale.ai All right reserved. This software is the confidential and proprietary information of
 * StarWhale.ai ("Confidential Information"). You shall not disclose such Confidential Information and shall use it only
 * in accordance with the terms of the license agreement you entered into with StarWhale.com.
 */

package ai.starwhale.mlops.api.protocol.job;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonValue;
import io.swagger.v3.oas.annotations.media.Schema;
import java.io.Serializable;
import java.util.List;
import javax.validation.Valid;
import lombok.Builder;
import lombok.Data;
import org.springframework.validation.annotation.Validated;

@Data
@Builder
@Validated
@Schema(description = "Job object", title = "Job")
public class JobVO implements Serializable {
    @JsonProperty("id")
    private String id;

    @JsonProperty("uuid")
    private String uuid;

    @JsonProperty("modelName")
    private String modelName;

    @JsonProperty("modelVersion")
    private String modelVersion;

    @JsonProperty("dataset")
    @Valid
    private List<String> dataset;

    @JsonProperty("baseImage")
    private String baseImage;

    @JsonProperty("device")
    private String device;

    @JsonProperty("deviceCount")
    private Integer deviceCount;

    @JsonProperty("ownerName")
    private String ownerName;

    @JsonProperty("createTime")
    private String createTime;

    @JsonProperty("duration")
    private String duration;

    @JsonProperty("stopTime")
    private String stopTime;

    /**
     * Gets or Sets status
     */
    public enum StatusEnum {
        PREPARING("preparing"),

        RUNNING("running"),

        COMPLETED("completed"),

        CANCELLING("cancelling"),

        CANCELLED("cancelled"),

        FAILED("failed");

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
    @JsonProperty("status")
    private StatusEnum status;
}
