/*
 * Copyright 2022.1-2022
 * StarWhale.ai All right reserved. This software is the confidential and proprietary information of
 * StarWhale.ai ("Confidential Information"). You shall not disclose such Confidential Information and shall use it only
 * in accordance with the terms of the license agreement you entered into with StarWhale.com.
 */

package ai.starwhale.mlops.api.protocol.swmp;

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
@Schema(description = "SWModelPackage information object", title = "ModelInfo")
@Validated
public class SWModelPackageInfoVO implements Serializable {

    @JsonProperty("modelName")
    private String modelName;

    @JsonProperty("files")
    @Valid
    private List<ModelFile> files;

    @Data
    @Builder
    @Validated
    public static class ModelFile implements Serializable{
        @JsonProperty("name")
        private String name;

        @JsonProperty("size")
        private String size;
    }
}
