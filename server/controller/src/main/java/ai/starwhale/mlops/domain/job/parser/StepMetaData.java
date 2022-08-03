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

package ai.starwhale.mlops.domain.job.parser;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.type.TypeFactory;
import com.fasterxml.jackson.databind.util.Converter;
import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@JsonAutoDetect(fieldVisibility = JsonAutoDetect.Visibility.ANY)
public class StepMetaData {
    /**
     * the name of job which would be executed at job yaml during running time
     */
    @JsonProperty("job_name")
    private String jobName;
    @JsonProperty("step_name")
    private String stepName;
    private Integer concurrency = 1;
    private List<String> dependency;
    @JsonDeserialize(contentConverter = ResourceConverter.class)
    private List<Resource> resources;
    @JsonProperty("task_num")
    private Integer taskNum = 1;
}

@Data
@Builder
class Resource {
    private String type;
    private Integer num;
}

class ResourceConverter implements Converter<String, Resource> {

    @Override
    public Resource convert(String value) {
        String[] res = value.split("=");
        return Resource.builder().type(res[0]).num(Integer.valueOf(res[1])).build();
    }

    @Override
    public JavaType getInputType(TypeFactory typeFactory) {
        return typeFactory.constructType(String.class);
    }

    @Override
    public JavaType getOutputType(TypeFactory typeFactory) {
        return typeFactory.constructType(Resource.class);
    }
}
