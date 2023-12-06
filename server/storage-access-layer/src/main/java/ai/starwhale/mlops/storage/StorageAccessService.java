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

package ai.starwhale.mlops.storage;

import ai.starwhale.mlops.storage.aliyun.StorageAccessServiceAliyun;
import ai.starwhale.mlops.storage.baidu.StorageAccessServiceBos;
import ai.starwhale.mlops.storage.fs.FsConfig;
import ai.starwhale.mlops.storage.fs.StorageAccessServiceFile;
import ai.starwhale.mlops.storage.ksyun.StorageAccessServiceKsyun;
import ai.starwhale.mlops.storage.memory.StorageAccessServiceMemory;
import ai.starwhale.mlops.storage.minio.StorageAccessServiceMinio;
import ai.starwhale.mlops.storage.qcloud.StorageAccessServiceQcloud;
import ai.starwhale.mlops.storage.s3.S3Config;
import ai.starwhale.mlops.storage.s3.StorageAccessServiceS3;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.InvocationTargetException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.apache.commons.lang3.tuple.Pair;

/**
 * provides file upload/ download /list services
 */
public interface StorageAccessService {

    class Registry {

        private static final Map<String, Class<? extends StorageAccessService>> storageAccessServiceClassMap =
                registerAll();

        private static Map<String, Class<? extends StorageAccessService>> registerAll() {
            Map<String, Class<? extends StorageAccessService>> ret = new HashMap<>();
            ret.put("memory", StorageAccessServiceMemory.class);

            ret.put("file", StorageAccessServiceFile.class);
            ret.put("fs", StorageAccessServiceFile.class);

            ret.put("s3", StorageAccessServiceS3.class);

            ret.put("minio", StorageAccessServiceMinio.class);

            ret.put("aliyun", StorageAccessServiceAliyun.class);
            ret.put("oss", StorageAccessServiceAliyun.class);

            ret.put("tencent", StorageAccessServiceQcloud.class);
            ret.put("qcloud", StorageAccessServiceQcloud.class);
            ret.put("cos", StorageAccessServiceQcloud.class);

            ret.put("baidu", StorageAccessServiceBos.class);
            ret.put("bos", StorageAccessServiceBos.class);

            ret.put("ksyun", StorageAccessServiceKsyun.class);
            ret.put("ks3", StorageAccessServiceKsyun.class);

            return ret;
        }

        public static Class<? extends StorageAccessService> getClassByType(String type) {
            return storageAccessServiceClassMap.get(type.toLowerCase());
        }

        public static List<String> getUriSchemasByClass(Class<? extends StorageAccessService> clazz) {
            return storageAccessServiceClassMap.entrySet().stream()
                    .filter(entry -> entry.getValue() == clazz)
                    .map(Entry::getKey)
                    .collect(Collectors.toList());
        }
    }


    Map<Pair<String, Object>, StorageAccessService> storageAccessServiceMap = new ConcurrentHashMap<>();


    static StorageAccessService getMemoryStorageAccessService() {
        return storageAccessServiceMap.computeIfAbsent(Pair.of("memory", null),
                key -> new StorageAccessServiceMemory());
    }

    static StorageAccessService getFileStorageAccessService(FsConfig config) {
        return storageAccessServiceMap.computeIfAbsent(Pair.of("file", config),
                key -> new StorageAccessServiceFile(config));
    }

    static StorageAccessService getS3LikeStorageAccessService(String type, S3Config config) {
        return storageAccessServiceMap.computeIfAbsent(Pair.of(type, config), key -> {
            var clazz = Registry.getClassByType(type.toLowerCase());
            if (clazz == null) {
                throw new IllegalArgumentException("invalid type " + type);
            }
            try {
                return clazz.getConstructor(S3Config.class).newInstance(config);
            } catch (NoSuchMethodException
                     | InstantiationException
                     | IllegalAccessException
                     | InvocationTargetException e) {
                throw new RuntimeException(e);
            }
        });
    }

    boolean compatibleWith(StorageUri uri);

    StorageObjectInfo head(String path) throws IOException;

    StorageObjectInfo head(String path, boolean md5sum) throws IOException;

    void put(String path, InputStream inputStream, long size) throws IOException;

    void put(String path, byte[] body) throws IOException;

    void put(String path, InputStream inputStream) throws IOException;

    LengthAbleInputStream get(String path) throws IOException;

    LengthAbleInputStream get(String path, Long offset, Long size) throws IOException;

    Stream<String> list(String path) throws IOException;

    void delete(String path) throws IOException;

    /**
     * return an accessible url using http get method
     *
     * @param path          the key of an object or path of a file
     * @param expTimeMillis the url will expire after expTimeMillis
     * @return pre-signed url of an object or http get accessible url
     * @throws IOException any possible IO exception
     */
    String signedUrl(String path, Long expTimeMillis) throws IOException;

    default List<String> signedUrlAllDomains(String path, Long expTimeMillis) throws IOException {
        return List.of(signedUrl(path, expTimeMillis));
    }

    String signedPutUrl(String path, String contentType, Long expTimeMillis) throws IOException;

    default List<String> signedPutUrlAllDomains(String path, String contentType, Long expTimeMillis)
            throws IOException {
        return List.of(signedPutUrl(path, contentType, expTimeMillis));
    }
}
