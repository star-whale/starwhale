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

package ai.starwhale.mlops.domain.model;

import ai.starwhale.mlops.domain.job.spec.StepSpec;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Starwhale Model Package
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Model {

    Long id;

    String name;

    String version;

    /**
     * The storage path of the model, it could be a directory or a single file
     */
    String path;

    List<StepSpec> stepSpecs;

}
