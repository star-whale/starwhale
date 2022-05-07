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

package ai.starwhale.mlops.domain.swds.upload;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;
import java.util.Map;
import lombok.Data;

/**
 * correspond to yaml file
 */
@JsonIgnoreProperties(ignoreUnknown = true)
@Data
public class Manifest{
    Build build;
    @JsonProperty("created_at")
    String createdAt;
    @JsonProperty("dataset_attr")
    DatasetAttr datasetAttr;
    @JsonProperty("dataset_byte_size")
    int datasetByteSize;
    Dep2 dep;
    Extra extra;
    String mode;
    String process;
    String version;
    String name;
    Map<String,String> signature;
    String rawYaml;
    @Data
    public static class Extra{
        String desc;
        List<String> tag;
    }
    @Data
    public class Build{
        String os;
        @JsonProperty("sw_version")
        String swVersion;
    }

    @Data
    public class DatasetAttr{

        @JsonProperty("alignment_size")
        int alignmentSize;
        @JsonProperty("batch_size")
        int batchSize;
        @JsonProperty("volume_size")
        int volumeSize;
    }

    @Data
    public static class Conda{
        boolean use;
    }

    @Data
    public static class Dep2{
        @JsonProperty("local_gen_env")
        boolean localGenEnv;
        Conda conda;
        Dep dep;
        String env;
        String python;
        String system;
        Venv venv;
    }

    @Data
    public static class Venv{
        boolean use;
    }

    @Data
    public static class Dep{
        @JsonProperty("local_gen_env")
        boolean localGenEnv;
    }
}


