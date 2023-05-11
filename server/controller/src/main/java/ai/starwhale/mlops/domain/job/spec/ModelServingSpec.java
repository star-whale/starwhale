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

package ai.starwhale.mlops.domain.job.spec;

import ai.starwhale.mlops.domain.runtime.RuntimeResource;
import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.dataformat.yaml.YAMLMapper;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
@EqualsAndHashCode
@JsonAutoDetect(fieldVisibility = JsonAutoDetect.Visibility.ANY)
@JsonIgnoreProperties(ignoreUnknown = true)
public class ModelServingSpec {
    private List<RuntimeResource> resources;

    private Map<String, String> envVars;

    public static ModelServingSpec fromYamlString(String content) throws JsonProcessingException {
        var yamlMapper = new YAMLMapper();
        ModelServingSpec obj =  yamlMapper.readValue(content, new TypeReference<>() {
        });

        // sort by type, order resource list
        obj.resources.sort(Comparator.comparing(RuntimeResource::getType));
        return obj;
    }

    public String dumps() throws JsonProcessingException {
        return new YAMLMapper().writeValueAsString(this);
    }
}
