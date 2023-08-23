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

package ai.starwhale.mlops.api.protocol.dataset;

import ai.starwhale.mlops.api.protocol.storage.FlattenFileVo;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import java.io.Serializable;
import java.util.List;
import javax.validation.Valid;
import javax.validation.constraints.NotNull;
import lombok.Builder;
import lombok.Data;
import org.springframework.validation.annotation.Validated;

@Data
@Builder
@Schema(description = "SWDataset information object", title = "DatasetInfo")
@Validated
public class DatasetInfoVo implements Serializable {

    @NotNull
    @JsonProperty("id")
    private String id;

    @NotNull
    @JsonProperty("name")
    private String name;

    @NotNull
    @JsonProperty("versionId")
    private String versionId;

    @NotNull
    @JsonProperty("versionName")
    private String versionName;

    @JsonProperty("versionAlias")
    private String versionAlias;

    /**
     * the table name for index in DataStore
     */
    String indexTable;

    @JsonProperty("versionTag")
    private String versionTag;

    @NotNull
    @JsonProperty("shared")
    private Integer shared;

    @NotNull
    @JsonProperty("createdTime")
    private Long createdTime;

    @JsonProperty("files")
    @Valid
    private List<FlattenFileVo> files;

    // leave this field for backward compatibility, client uses this field
    @NotNull
    @JsonProperty("versionMeta")
    private String versionMeta;

    private DatasetVersionVo versionInfo;
}
