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

package ai.starwhale.mlops.api.protocol.project;

import static ai.starwhale.mlops.domain.project.ProjectService.PROJECT_NAME_REGEX;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.io.Serializable;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Pattern;
import lombok.Data;
import org.springframework.validation.annotation.Validated;

@Data
@Validated
public class CreateProjectRequest implements Serializable {


    @JsonProperty("projectName")
    @NotNull
    @Pattern(regexp = PROJECT_NAME_REGEX, message = "Project name is invalid.")
    private String projectName;

    @NotNull
    @JsonProperty("privacy")
    private String privacy;

    @NotNull
    @JsonProperty("description")
    private String description;

}
