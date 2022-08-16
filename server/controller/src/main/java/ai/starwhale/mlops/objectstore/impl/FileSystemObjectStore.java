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
package ai.starwhale.mlops.objectstore.impl;

import ai.starwhale.mlops.memory.BufferManager;
import ai.starwhale.mlops.memory.SwBuffer;
import ai.starwhale.mlops.objectstore.ObjectStore;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.security.InvalidParameterException;
import java.text.MessageFormat;

@Slf4j
public class FileSystemObjectStore implements ObjectStore {
    @Autowired
    private BufferManager bufferManager;

    @Override
    public void put(String name, SwBuffer buf) throws IOException {
        new FileOutputStream(name).getChannel().write(buf.asByteBuffer());
    }

    @Override
    public SwBuffer get(String name) throws IOException {
        long size = Files.size(Paths.get(name));
        if (size > 64 * 1024 * 1024) {
            throw new InvalidParameterException(
                    MessageFormat.format("file {0} is too large, size {1}", name, size));
        }
        var buf = this.bufferManager.allocate((int) size);
        new FileInputStream(name).getChannel().read(buf.asByteBuffer());
        return buf;
    }
}
