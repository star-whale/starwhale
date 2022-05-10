/**
 * Copyright 2022 Starwhale, Inc. All Rights Reserved.
 * <p>
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * <p>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package ai.starwhale.mlops.agent.node;

import org.apache.commons.lang3.StringUtils;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.UUID;

public class UniqueID {
    public static String UNIQUE_FILE = "unique-id";

    private String id;
    private final String basePath;

    public UniqueID(String basePath) {
        this.basePath = basePath;
    }

    public synchronized String id() throws IOException {
        if (StringUtils.isNotEmpty(id))
            return id;
        return getOrCheckUniqueID();
    }

    private String getOrCheckUniqueID() throws IOException {
        String uniqueIDFile = basePath + File.separator + UNIQUE_FILE;
        Path filePath = Path.of(uniqueIDFile);
        if (Files.exists(filePath)) {
            String content = Files.readString(filePath);
            if (StringUtils.isNotEmpty(content)) {
                id = content;
                return id;
            }
        }
        id = UUID.randomUUID().toString();
        Files.writeString(filePath, id, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
        return id;
    }
}
