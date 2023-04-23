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

package ai.starwhale.mlops.domain.system.resourcepool.bo;

import ai.starwhale.mlops.domain.runtime.RuntimeResource;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class Resource {

    String name;
    Float max;
    Float min;
    Float defaults;

    public Resource(String name) {
        this.name = name;
    }

    public void validate(RuntimeResource runtimeResource) {
        var req = runtimeResource.getRequest();
        if (req == null) {
            return;
        }
        if (max != null && req > max) {
            throw new IllegalArgumentException(String.format("request value is too large, max is %.1f", max));
        }
        if (min != null && req < min) {
            throw new IllegalArgumentException(String.format("value is too small, min is %.1f", min));
        }
    }

    public void patch(RuntimeResource runtimeResource) {
        if (runtimeResource == null) {
            return;
        }
        if (runtimeResource.getRequest() == null) {
            runtimeResource.setRequest(defaults);
        }
    }
}
