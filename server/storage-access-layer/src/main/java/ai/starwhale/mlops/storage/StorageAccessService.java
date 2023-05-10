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

import java.io.IOException;
import java.io.InputStream;
import java.util.stream.Stream;

/**
 * provides file upload/ download /list services
 */
public interface StorageAccessService {

    StorageObjectInfo head(String path) throws IOException;

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

    String signedPutUrl(String path, Long expTimeMillis) throws IOException;
}
