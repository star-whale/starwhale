/*
 * Copyright 2022.1-2022
 * StarWhale.ai All right reserved. This software is the confidential and proprietary information of
 * StarWhale.ai ("Confidential Information"). You shall not disclose such Confidential Information and shall use it only
 * in accordance with the terms of the license agreement you entered into with StarWhale.com.
 */

package ai.starwhale.mlops.api.protocol.swmp;

import ai.starwhale.mlops.api.protocol.user.UserVO;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import java.io.Serializable;
import lombok.Builder;
import lombok.Data;
import org.springframework.validation.annotation.Validated;

@Data
@Builder
@Schema(description = "Model version object", title = "ModelVersion")
@Validated
public class SWModelPackageVersionVO implements Serializable {

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

    public static SWModelPackageVersionVO empty() {
        return new SWModelPackageVersionVO("", "", "", "{}", -1L, UserVO.empty());
    }
}
