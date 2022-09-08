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

import ai.starwhale.mlops.memory.SwBuffer;
import ai.starwhale.mlops.memory.SwBufferManager;
import ai.starwhale.mlops.objectstore.ObjectStore;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.security.InvalidParameterException;
import java.text.MessageFormat;
import java.util.Iterator;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@ConditionalOnProperty(prefix = "sw.storage", name = "type", havingValue = "fs")
public class FileSystemObjectStore implements ObjectStore {

    private final SwBufferManager bufferManager;

    private final String rootDir;

    public FileSystemObjectStore(SwBufferManager bufferManager, @Value("${sw.datastore.fsRootDir}") String rootDir) {
        this.bufferManager = bufferManager;
        this.rootDir = rootDir;
    }

    @Override
    public Iterator<String> list(String prefix) throws IOException {
        return new FileIterator(this.rootDir, prefix);
    }

    @Override
    public void put(String name, SwBuffer buf) throws IOException {
        var f = new File(this.rootDir, name);
        //noinspection ResultOfMethodCallIgnored
        f.getParentFile().mkdirs();
        var temp = File.createTempFile("sw_tmp", null);
        try (var channel = new FileOutputStream(temp).getChannel()) {
            channel.write(buf.asByteBuffer());
            channel.close();
            Files.move(temp.toPath(), f.toPath(), StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
        } finally {
            //noinspection ResultOfMethodCallIgnored
            temp.delete();
        }
    }

    @Override
    public SwBuffer get(String name) throws IOException {
        var f = new File(this.rootDir, name);
        if (!f.exists()) {
            throw new FileNotFoundException(f.getAbsolutePath());
        }
        long size = Files.size(f.toPath());
        if (size > 64 * 1024 * 1024) {
            throw new InvalidParameterException(
                    MessageFormat.format("file {0} is too large, size {1}", name, size));
        }
        var buf = this.bufferManager.allocate((int) size);
        try (var channel = new FileInputStream(f).getChannel()) {
            channel.read(buf.asByteBuffer());
        }
        return buf;
    }

    @Override
    public void delete(String name) {
        //noinspection ResultOfMethodCallIgnored
        new File(this.rootDir, name).delete();
    }
}
