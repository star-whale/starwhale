/**
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

package ai.starwhale.mlops.domain.job;

import ai.starwhale.mlops.common.BaseEntity;
import ai.starwhale.mlops.domain.job.status.JobStatus;
import ai.starwhale.mlops.domain.project.ProjectEntity;
import ai.starwhale.mlops.domain.swds.SWDatasetVersionEntity;
import ai.starwhale.mlops.domain.swmp.SWModelPackageVersionEntity;
import ai.starwhale.mlops.domain.user.UserEntity;
import java.time.LocalDateTime;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

@EqualsAndHashCode(callSuper = true)
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class JobEntity extends BaseEntity {

    private Long id;

    private String jobUuid;

    private String modelName;

    private Long projectId;

    private ProjectEntity project;

    private Long swmpVersionId;

    private SWModelPackageVersionEntity swmpVersion;

    private Long ownerId;

    private UserEntity owner;

    private LocalDateTime createdTime;

    private LocalDateTime finishedTime;

    private Long durationMs;

    private JobStatus jobStatus;

    private Long baseImageId;

    private BaseImageEntity baseImage;

    private Integer deviceType;

    private Integer deviceAmount;

    private String resultOutputPath;


}
