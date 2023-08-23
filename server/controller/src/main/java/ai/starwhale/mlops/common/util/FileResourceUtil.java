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

package ai.starwhale.mlops.common.util;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import org.springframework.util.StringUtils;

public class FileResourceUtil {

    /**
     * getJobDefaultTemplate returns the template content, prefer using the sysFsPath than fallbackRc
     *
     * @param sysFsPath  system filesystem path, return the filesystem file contents when not empty
     * @param fallbackRc resource path when system filesystem path not specified
     * @return template file content
     * @throws IOException when reading template file
     */
    public static String getFileContent(String sysFsPath, String fallbackRc) throws IOException {
        if (StringUtils.hasText(sysFsPath)) {
            return Files.readString(Paths.get(sysFsPath));
        }
        InputStream is = FileResourceUtil.class.getClassLoader().getResourceAsStream(fallbackRc);
        return new String(is.readAllBytes(), StandardCharsets.UTF_8);
    }

}
