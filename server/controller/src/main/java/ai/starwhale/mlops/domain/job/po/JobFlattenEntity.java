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

import static ai.starwhale.mlops.domain.job.JobSchema.CommentColumn;
import static ai.starwhale.mlops.domain.job.JobSchema.CreatedTimeColumn;
import static ai.starwhale.mlops.domain.job.JobSchema.DataSetsColumn;
import static ai.starwhale.mlops.domain.job.JobSchema.DataSetIdVersionMapColumn;
import static ai.starwhale.mlops.domain.job.JobSchema.DevModeColumn;
import static ai.starwhale.mlops.domain.job.JobSchema.DevWayColumn;
import static ai.starwhale.mlops.domain.job.JobSchema.DurationColumn;
import static ai.starwhale.mlops.domain.job.JobSchema.FinishTimeColumn;
import static ai.starwhale.mlops.domain.job.JobSchema.IsDeletedColumn;
import static ai.starwhale.mlops.domain.job.JobSchema.JobStatusColumn;
import static ai.starwhale.mlops.domain.job.JobSchema.JobTypeColumn;
import static ai.starwhale.mlops.domain.job.JobSchema.KeyColumn;
import static ai.starwhale.mlops.domain.job.JobSchema.LongIdColumn;
import static ai.starwhale.mlops.domain.job.JobSchema.ModelNameColumn;
import static ai.starwhale.mlops.domain.job.JobSchema.ModelVersionColumn;
import static ai.starwhale.mlops.domain.job.JobSchema.ModelVersionIdColumn;
import static ai.starwhale.mlops.domain.job.JobSchema.ModifiedTimeColumn;
import static ai.starwhale.mlops.domain.job.JobSchema.NameColumn;
import static ai.starwhale.mlops.domain.job.JobSchema.OwnerIdColumn;
import static ai.starwhale.mlops.domain.job.JobSchema.OwnerNameColumn;
import static ai.starwhale.mlops.domain.job.JobSchema.ProjectIdColumn;
import static ai.starwhale.mlops.domain.job.JobSchema.ResourcePoolColumn;
import static ai.starwhale.mlops.domain.job.JobSchema.ResultOutputPathColumn;
import static ai.starwhale.mlops.domain.job.JobSchema.RuntimeNameColumn;
import static ai.starwhale.mlops.domain.job.JobSchema.RuntimeVersionColumn;
import static ai.starwhale.mlops.domain.job.JobSchema.RuntimeVersionIdColumn;
import static ai.starwhale.mlops.domain.job.JobSchema.StepSpecColumn;

import ai.starwhale.mlops.domain.job.DevWay;
import ai.starwhale.mlops.domain.job.JobType;
import ai.starwhale.mlops.domain.job.status.JobStatus;
import ai.starwhale.mlops.domain.model.bo.ModelVersion;
import ai.starwhale.mlops.domain.project.bo.Project;
import ai.starwhale.mlops.domain.user.bo.User;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.Date;
import java.util.List;
import java.util.Map;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

@EqualsAndHashCode
@Data
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class JobFlattenEntity {

    @JsonProperty(LongIdColumn)
    private Long id;

    @JsonProperty(KeyColumn)
    private String jobUuid;

    @JsonProperty(NameColumn)
    private String name;

    @JsonProperty(ProjectIdColumn)
    private Long projectId;

    private Project project;

    @JsonProperty(ModelVersionIdColumn)
    private Long modelVersionId;

    @JsonProperty(ModelNameColumn)
    private String modelName;

    @JsonProperty(ModelVersionColumn)
    private String modelVersionValue;

    private ModelVersion modelVersion;

    @JsonProperty(OwnerIdColumn)
    private Long ownerId;

    @JsonProperty(OwnerNameColumn)
    private String ownerName;

    private User owner;

    @JsonProperty(CreatedTimeColumn)
    private Date createdTime;

    @JsonProperty(ModifiedTimeColumn)
    private Date modifiedTime;

    @JsonProperty(FinishTimeColumn)
    private Date finishedTime;

    @JsonProperty(DurationColumn)
    private Long durationMs;

    @JsonProperty(JobStatusColumn)
    private JobStatus jobStatus;

    @JsonProperty(RuntimeVersionIdColumn)
    private Long runtimeVersionId;

    @JsonProperty(RuntimeNameColumn)
    private String runtimeName;

    @JsonProperty(RuntimeVersionColumn)
    private String runtimeVersionValue;

    @JsonProperty(DataSetIdVersionMapColumn)
    private Map<Long, String> datasetIdVersionMap;

    @JsonProperty(DataSetsColumn)
    private List<Object> datasets;

    @JsonProperty(ResultOutputPathColumn)
    private String resultOutputPath;

    @JsonProperty(CommentColumn)
    private String comment;

    @JsonProperty(JobTypeColumn)
    private JobType type;

    @JsonProperty(ResourcePoolColumn)
    private String resourcePool;

    @JsonProperty(StepSpecColumn)
    private String stepSpec;

    @JsonProperty(IsDeletedColumn)
    private Integer isDeleted;

    @JsonProperty(DevModeColumn)
    private boolean devMode;

    @JsonProperty(DevWayColumn)
    private DevWay devWay;

    // don't sync it to datastore
    private String devPassword;

    private Date autoReleaseTime;
}
