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

package ai.starwhale.mlops.storage.fs;

import ai.starwhale.mlops.storage.LengthAbleInputStream;
import ai.starwhale.mlops.storage.StorageAccessService;
import ai.starwhale.mlops.storage.StorageObjectInfo;
import com.google.common.io.ByteStreams;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.RandomAccessFile;
import java.nio.channels.Channels;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Spliterator;
import java.util.Spliterators;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;
import org.apache.commons.compress.utils.BoundedInputStream;

public class StorageAccessServiceFile implements StorageAccessService {

    private final File rootDir;

    private final String serviceProvider;

    /**
     * @param fsConfig fsConfig
     */
    public StorageAccessServiceFile(FsConfig fsConfig) {
        this.rootDir = new File(fsConfig.getRootDir());
        this.serviceProvider = fsConfig.getServiceProvider();
        if (!this.rootDir.exists()) {
            throw new IllegalArgumentException(rootDir + " does not exist");
        }
        if (!this.rootDir.isDirectory()) {
            throw new IllegalArgumentException(rootDir + " is not a directory");
        }
    }

    @Override
    public StorageObjectInfo head(String path) throws IOException {
        var f = new File(this.rootDir, path);
        if (!f.exists()) {
            return new StorageObjectInfo(false, 0L, null);
        }
        return new StorageObjectInfo(true, f.length(), null);
    }

    @Override
    public void put(String path, InputStream inputStream, long size) throws IOException {
        this.put(path, new BoundedInputStream(inputStream, size));
    }

    @Override
    public void put(String path, InputStream inputStream) throws IOException {
        var f = new File(this.rootDir, path);
        //noinspection ResultOfMethodCallIgnored
        f.getParentFile().mkdirs();
        var temp = File.createTempFile("sw_tmp", null, rootDir);
        try (var out = new FileOutputStream(temp)) {
            inputStream.transferTo(out);
            out.close();
            Files.move(temp.toPath(), f.toPath(), StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
        } finally {
            //noinspection ResultOfMethodCallIgnored
            temp.delete();
        }
    }

    @Override
    public void put(String path, byte[] body) throws IOException {
        this.put(path, new ByteArrayInputStream(body), body.length);
    }

    @Override
    public LengthAbleInputStream get(String path) throws IOException {
        var f = new File(this.rootDir, path);
        if (!f.exists()) {
            throw new FileNotFoundException(f.getAbsolutePath());
        }
        return new LengthAbleInputStream(new FileInputStream(f), f.length());
    }

    @Override
    public LengthAbleInputStream get(String path, Long offset, Long size) throws IOException {
        if (offset == null || offset < 0) {
            offset = 0L;
        }
        if (size == null || size < 0) {
            size = -1L;
        }
        var f = new RandomAccessFile(new File(this.rootDir, path), "r");
        f.seek(offset);
        //noinspection UnstableApiUsage
        var is = ByteStreams.limit(Channels.newInputStream(f.getChannel()), size);
        return new LengthAbleInputStream(is, size);
    }

    @Override
    public Stream<String> list(String path) {
        return StreamSupport.stream(
                Spliterators.spliteratorUnknownSize(
                        new FileIterator(this.rootDir, path),
                        Spliterator.ORDERED),
                false);
    }

    @Override
    public void delete(String path) throws IOException {
        var f = new File(this.rootDir, path);
        while (f.delete()) {
            f = f.getParentFile();
            if (f.equals(this.rootDir) || !f.isDirectory()) {
                return;
            }
            try (Stream<Path> entries = Files.list(f.toPath())) {
                if (entries.findFirst().isPresent()) {
                    return;
                }
            }
        }
    }

    @Override
    public String signedUrl(String path, Long expTimeMillis) throws IOException {
        return serviceProvider + "/" + path + "/" + (System.currentTimeMillis() + expTimeMillis);
    }

    @Override
    public String signedPutUrl(String path, Long expTimeMillis) throws IOException {
        return serviceProvider + "/" + path + "/" + (System.currentTimeMillis() + expTimeMillis);
    }
}
