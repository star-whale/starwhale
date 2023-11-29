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

package ai.starwhale.mlops.domain.job.bo;

import ai.starwhale.mlops.domain.job.BizType;
import ai.starwhale.mlops.domain.job.DevWay;
import java.util.List;
import javax.validation.constraints.NotNull;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.SuperBuilder;

@Data
@SuperBuilder
@EqualsAndHashCode(callSuper = true)
public class UserJobCreateRequest extends JobCreateRequest {
    @NotNull
    Long modelVersionId;

    String handler;

    @NotNull
    Long runtimeVersionId;

    List<Long> datasetVersionIds;

    BizType bizType;
    String bizId;

    boolean devMode;
    DevWay devWay;
    String devPassword;
    Long ttlInSec;
}
