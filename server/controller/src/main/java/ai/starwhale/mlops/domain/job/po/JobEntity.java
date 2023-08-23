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

package ai.starwhale.mlops.domain.job.po;

import ai.starwhale.mlops.common.BaseEntity;
import ai.starwhale.mlops.domain.bundle.base.BundleEntity;
import ai.starwhale.mlops.domain.job.DevWay;
import ai.starwhale.mlops.domain.job.JobType;
import ai.starwhale.mlops.domain.job.status.JobStatus;
import ai.starwhale.mlops.domain.model.po.ModelVersionEntity;
import ai.starwhale.mlops.domain.project.po.ProjectEntity;
import ai.starwhale.mlops.domain.user.po.UserEntity;
import java.util.Date;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

@EqualsAndHashCode
@Data
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
public class JobEntity extends BaseEntity implements BundleEntity {

    private Long id;

    private String name;

    private String jobUuid;

    private String modelName;

    private Long projectId;

    private ProjectEntity project;

    private Long modelVersionId;

    private ModelVersionEntity modelVersion;

    private Long ownerId;

    private UserEntity owner;

    @Builder.Default
    private Date finishedTime = defaultDate;

    private Long durationMs;

    private JobStatus jobStatus;

    private Long runtimeVersionId;

    private String resultOutputPath;

    private String comment;

    private Integer isDeleted;

    private JobType type;

    private String resourcePool;

    private String stepSpec;

    private boolean devMode;
    private DevWay devWay;
    private String devPassword;
    private Date autoReleaseTime;

    private Date pinnedTime;
    private String virtualJobName;

    @Override
    public String getName() {
        return jobUuid;
    }
}
