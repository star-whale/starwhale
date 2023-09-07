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

package ai.starwhale.mlops.api.protocol.report;

import ai.starwhale.mlops.api.protocol.user.UserVo;
import io.swagger.v3.oas.annotations.media.Schema;
import javax.validation.constraints.NotNull;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
@Schema(description = "Report object", title = "Report")
public class ReportVo {

    @NotNull
    private Long id;

    @NotNull
    private String uuid;

    @NotNull
    private String title;

    private String content;

    private String description;

    private Boolean shared;

    @NotNull
    private UserVo owner;

    @NotNull
    private Long createdTime;

    @NotNull
    private Long modifiedTime;

    public static ReportVo empty() {
        return new ReportVo(null, "", "", "", "", false, null, null, null);
    }
}
