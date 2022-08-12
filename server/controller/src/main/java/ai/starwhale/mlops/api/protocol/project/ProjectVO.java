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

import ai.starwhale.mlops.api.protocol.user.UserVO;
import io.swagger.v3.oas.annotations.media.Schema;
import java.io.Serializable;
import lombok.Builder;
import lombok.Data;
import org.springframework.validation.annotation.Validated;

@Data
@Builder
@Schema(description = "Project object", title = "Project")
@Validated
public class ProjectVO implements Serializable {

    private String id;

    private String name;

    private String description;

    private Long createdTime;

    private UserVO owner;

    private Boolean isDefault;

    public static ProjectVO empty() {
        return new ProjectVO("", "", "", -1L, UserVO.empty(), false);
    }

    public static ProjectVO system() {
        return new ProjectVO("0", "SYSTEM", "System",-1L, UserVO.empty(), false);
    }
}
