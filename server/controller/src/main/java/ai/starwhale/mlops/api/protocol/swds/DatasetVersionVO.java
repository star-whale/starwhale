/*
 * Copyright 2022.1-2022
 * StarWhale.ai All right reserved. This software is the confidential and proprietary information of
 * StarWhale.ai ("Confidential Information"). You shall not disclose such Confidential Information and shall use it only
 * in accordance with the terms of the license agreement you entered into with StarWhale.com.
 */

package ai.starwhale.mlops.api.protocol.swds;

import ai.starwhale.mlops.api.protocol.user.UserVO;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import java.io.Serializable;
import lombok.Builder;
import lombok.Data;
import org.springframework.validation.annotation.Validated;

@Data
@Builder
@Validated
@Schema(description = "Dataset version object", title = "DatasetVersion")
public class DatasetVersionVO implements Serializable {
    @JsonProperty("id")
    private String id;

    @JsonProperty("name")
    private String name;

    @JsonProperty("tag")
    private String tag;

    @JsonProperty("meta")
    private Object meta;

    @JsonProperty("createdTime")
    private Long createdTime;

    @JsonProperty("owner")
    private UserVO owner;
}
