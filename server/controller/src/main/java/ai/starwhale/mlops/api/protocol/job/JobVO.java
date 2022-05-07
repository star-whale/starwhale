/*
 * Copyright 2022.1-2022
 * StarWhale.ai All right reserved. This software is the confidential and proprietary information of
 * StarWhale.ai ("Confidential Information"). You shall not disclose such Confidential Information and shall use it only
 * in accordance with the terms of the license agreement you entered into with StarWhale.com.
 */

package ai.starwhale.mlops.api.protocol.job;

import ai.starwhale.mlops.api.protocol.runtime.BaseImageVO;
import ai.starwhale.mlops.api.protocol.user.UserVO;
import ai.starwhale.mlops.domain.job.status.JobStatus;
import com.fasterxml.jackson.annotation.JsonProperty;
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

    @JsonProperty("datasets")
    @Valid
    private List<String> datasets;

    @JsonProperty("baseImage")
    private BaseImageVO baseImage;

    @JsonProperty("device")
    private String device;

    @JsonProperty("deviceAmount")
    private Integer deviceAmount;

    @JsonProperty("owner")
    private UserVO owner;

    @JsonProperty("createdTime")
    private Long createdTime;

    @JsonProperty("stopTime")
    private Long stopTime;

    @JsonProperty("jobStatus")
    private JobStatus jobStatus;

    @JsonProperty("duration")
    public Long getDuration(){
        if(null == stopTime || stopTime <=0){
            return System.currentTimeMillis() - createdTime;
        }
        return stopTime - createdTime;
    }

}
