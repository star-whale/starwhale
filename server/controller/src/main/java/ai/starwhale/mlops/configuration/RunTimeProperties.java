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

package ai.starwhale.mlops.configuration;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Data
@AllArgsConstructor
@NoArgsConstructor
@ConfigurationProperties(prefix = "sw.runtime")
public class RunTimeProperties {

    String imageDefault;
    ImageBuild imageBuild;
    Pypi pypi;

    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    public static class ImageBuild {

        String resourcePool;
        String image;

        public static ImageBuild empty() {
            return new ImageBuild("", "");
        }
    }

    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    public static class Pypi {

        String indexUrl;
        String extraIndexUrl;
        String trustedHost;

        public static Pypi empty() {
            return new Pypi("", "", "");
        }
    }

}
