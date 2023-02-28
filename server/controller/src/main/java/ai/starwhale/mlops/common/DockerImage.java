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

package ai.starwhale.mlops.common;

import java.util.regex.Matcher;
import java.util.regex.Pattern;
import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.ToString;
import org.springframework.util.StringUtils;

@Getter
@ToString
@EqualsAndHashCode
@AllArgsConstructor
@NoArgsConstructor
public class DockerImage {

    String registry;
    String image;

    /**
     * please refer to https://github.com/distribution/distribution/blob/v2.7.1/reference/reference.go
     */
    private static final Pattern PATTERN_IMAGE_FULL = Pattern.compile("^(.+?)\\/(.+)$");

    /**
     * @param imageNameFull such as ghcr.io/starwhale-ai/starwhale:0.3.5-rc123.dev12432344
     */
    public DockerImage(String imageNameFull) {
        Matcher matcher = PATTERN_IMAGE_FULL.matcher(imageNameFull);
        if (!matcher.matches()) {
            this.registry = "";
            this.image = imageNameFull;
        } else {
            String candidateRegistry = matcher.group(1);
            if (isDomain(candidateRegistry)) {
                this.registry = candidateRegistry;
                image = matcher.group(2);
            } else {
                this.registry = "";
                this.image = imageNameFull;
            }

        }
    }

    private static final Pattern PATTERN_DOMAIN_LOCAL_HOST = Pattern.compile("localhost(:\\d+)?");

    private static boolean isDomain(String candidate) {
        return candidate.contains(".") || PATTERN_DOMAIN_LOCAL_HOST.matcher(candidate).matches();
    }

    private static final String SLASH = "/";

    public String resolve(String newRegistry) {
        if (!StringUtils.hasText(newRegistry)) {
            newRegistry = this.registry;
        }
        if (!StringUtils.hasText(newRegistry)) {
            return image;
        }
        return StringUtils.trimTrailingCharacter(newRegistry, '/') + SLASH + image;
    }

}
