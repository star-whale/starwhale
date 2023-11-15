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

import ai.starwhale.mlops.domain.job.BizType;
import ai.starwhale.mlops.domain.job.DevWay;
import ai.starwhale.mlops.domain.job.JobType;
import ai.starwhale.mlops.domain.job.status.JobStatus;
import ai.starwhale.mlops.domain.model.bo.ModelVersion;
import ai.starwhale.mlops.domain.project.bo.Project;
import ai.starwhale.mlops.domain.user.bo.User;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.util.Date;
import java.util.List;
import java.util.Map;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

@EqualsAndHashCode
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class JobFlattenEntity {

    private Long id;

    private String jobUuid;

    private String name;

    private Long projectId;

    private Project project;

    private Long modelVersionId;
    private String modelName;
    private String modelVersionValue;
    private String modelUriForView;
    private String modelUri;

    private ModelVersion modelVersion;

    private Long ownerId;

    private String ownerName;

    private User owner;

    private Date createdTime;

    private Date modifiedTime;

    private Date finishedTime;

    private Long durationMs;

    private JobStatus jobStatus;

    private Long runtimeVersionId;
    private String runtimeName;
    private String runtimeVersionValue;
    private String runtimeUriForView;
    private String runtimeUri;

    private Map<Long, String> datasetIdVersionMap;

    private String datasetsForView;
    private List<String> datasets;

    private String resultOutputPath;

    private String comment;

    private BizType bizType;
    private String bizId;

    private JobType type;

    private String resourcePool;

    private String stepSpec;

    private Integer isDeleted;

    private boolean devMode;

    private DevWay devWay;

    // don't sync it to datastore
    private String devPassword;

    private Date autoReleaseTime;
}
