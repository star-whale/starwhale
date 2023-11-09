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

package ai.starwhale.mlops.domain.dataset.build.bo;

import ai.starwhale.mlops.domain.dataset.build.BuildType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
public class CreateBuildRecordRequest {
    private String projectUrl;
    private String datasetName;
    private Boolean shared;
    private BuildType type;
    private String storagePath;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Csv {
        public enum Dialect {
            EXCEL("excel"), EXCEL_TAB("excel-tab"), UNIX("unix");

            private final String dialect;

            Dialect(String dialect) {
                this.dialect = dialect;
            }

            public String toString() {
                return this.dialect;
            }
        }

        private Dialect dialect;
        private String delimiter;
        private String quoteChar;
    }

    private Csv csv;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Json {
        private String fieldSelector;
    }

    private Json json;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class HuggingFace {
        private String repo;
        private String subset;
        private String split;
        private String revision;
    }

    private HuggingFace huggingFace;
}
