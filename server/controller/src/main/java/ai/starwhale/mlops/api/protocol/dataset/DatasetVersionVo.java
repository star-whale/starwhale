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

import ai.starwhale.mlops.api.protocol.user.UserVo;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import java.io.Serializable;
import java.util.List;
import lombok.Builder;
import lombok.Data;
import org.springframework.validation.annotation.Validated;

@Data
@Builder
@Validated
@Schema(description = "Dataset version object", title = "DatasetVersion")
public class DatasetVersionVo implements Serializable {

    @JsonProperty("id")
    private String id;

    @JsonProperty("name")
    private String name;

    private List<String> tags;

    @JsonProperty("alias")
    private String alias;

    private Boolean latest;

    @JsonProperty("meta")
    private Object meta;

    @JsonProperty("createdTime")
    private Long createdTime;

    @JsonProperty("owner")
    private UserVo owner;

    @JsonProperty("shared")
    private Integer shared;

    /**
     * the table name for index in DataStore
     */
    String indexTable;
}
