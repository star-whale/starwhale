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

package ai.starwhale.mlops.domain.dataset.upload.bo;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;
import lombok.Data;

/**
 * correspond to yaml file
 */
@JsonIgnoreProperties(ignoreUnknown = true)
@Data
public class Manifest {

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
    @JsonProperty("data_datastore_revision")
    String revision;
    @JsonProperty("dataset_summary")
    DatasetSummary datasetSummary;
    List<String> signature;
    String rawYaml;

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class DatasetSummary {

        @JsonProperty("data_byte_size")
        Long dataByteSize;

        @JsonProperty("data_format_type")
        String dataFormatType;

        @JsonProperty("increased_rows")
        Long increasedRows;

        @JsonProperty("label_byte_size")
        Long labelByteSize;

        @JsonProperty("object_store_type")
        String objectStoreType;

        @JsonProperty("rows")
        Long rows;

        @JsonProperty("unchanged_rows")
        Long unchangedRows;
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    @Data
    public static class Extra {

        String desc;
        List<String> tag;
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    @Data
    public static class Build {

        String os;
        @JsonProperty("sw_version")
        String swVersion;
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    @Data
    public static class DatasetAttr {

        @JsonProperty("alignment_size")
        int alignmentSize;
        @JsonProperty("batch_size")
        int batchSize;
        @JsonProperty("volume_size")
        int volumeSize;
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    @Data
    public static class Conda {

        boolean use;
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    @Data
    public static class Dep2 {

        @JsonProperty("local_gen_env")
        boolean localGenEnv;
        Conda conda;
        Dep dep;
        String env;
        String python;
        String system;
        Venv venv;
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    @Data
    public static class Venv {

        boolean use;
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    @Data
    public static class Dep {

        @JsonProperty("local_gen_env")
        boolean localGenEnv;
    }
}
