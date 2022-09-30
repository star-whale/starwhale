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

import ai.starwhale.mlops.exception.SwValidationException;
import ai.starwhale.mlops.exception.SwValidationException.ValidSubject;
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

    private static final Pattern PATTERN_IMAGE_FULL = Pattern.compile(
            "^(([a-z0-9-]+\\.[a-z0-9.]+)(:(\\d{2,5}))?)?\\/?(.+)$");

    /**
     * @param imageNameFull ghcr.io/starwhale-ai/starwhale:0.3.5-rc123.dev12432344
     */
    public DockerImage(String imageNameFull) {
        Matcher matcher = PATTERN_IMAGE_FULL.matcher(imageNameFull);
        if (!matcher.matches()) {
            throw new SwValidationException(ValidSubject.SETTING).tip("image style unknown");
        }
        registry = matcher.group(1);
        image = matcher.group(5);
    }

    private static final String SLASH = "/";

    public String resolve(String newRegistry) {
        if (!StringUtils.hasText(newRegistry)) {
            return image;
        }
        if (newRegistry.endsWith(SLASH)) {
            newRegistry = removeEndingSlash(newRegistry);
        }
        return newRegistry + SLASH + image;
    }

    private String removeEndingSlash(String newRegistry) {
        if (newRegistry.endsWith(SLASH)) {
            return removeEndingSlash(newRegistry.substring(0, newRegistry.length() - 1));
        }
        return newRegistry;
    }
}
