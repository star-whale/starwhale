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

package ai.starwhale.mlops.storage.minio;

import ai.starwhale.mlops.storage.LengthAbleInputStream;
import ai.starwhale.mlops.storage.StorageAccessService;
import ai.starwhale.mlops.storage.StorageObjectInfo;
import ai.starwhale.mlops.storage.s3.S3Config;
import ai.starwhale.mlops.storage.util.MetaHelper;
import io.minio.GetObjectArgs;
import io.minio.GetObjectResponse;
import io.minio.GetPresignedObjectUrlArgs;
import io.minio.ListObjectsArgs;
import io.minio.MinioClient;
import io.minio.PutObjectArgs;
import io.minio.RemoveObjectArgs;
import io.minio.StatObjectArgs;
import io.minio.StatObjectResponse;
import io.minio.errors.ErrorResponseException;
import io.minio.errors.MinioException;
import io.minio.http.Method;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.stream.Stream;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class StorageAccessServiceMinio implements StorageAccessService {

    private final String bucket;

    private final long partSize;

    private final MinioClient minioClient;

    public StorageAccessServiceMinio(S3Config s3Config) {
        this.bucket = s3Config.getBucket();
        this.partSize = s3Config.getHugeFilePartSize();
        minioClient =
                MinioClient.builder()
                        .endpoint(s3Config.getEndpoint())
                        .credentials(s3Config.getAccessKey(), s3Config.getSecretKey())
                        .build();
    }

    @Override
    public StorageObjectInfo head(String path) throws IOException {
        try {
            StatObjectResponse resp = this.minioClient.statObject(
                    StatObjectArgs.builder().bucket(bucket).object(path).build());
            return new StorageObjectInfo(true, resp.size(), MetaHelper.mapToString(resp.userMetadata()));
        } catch (ErrorResponseException e) {
            if ("NoSuchKey".equals(e.errorResponse().code())) {
                return new StorageObjectInfo(false, null, null);
            } else {
                log.error("head object fails", e);
                throw new IOException(e);
            }
        } catch (Exception e) {
            log.error("head object fails", e);
            throw new IOException(e);
        }
    }

    @Override
    public void put(String path, InputStream inputStream, long size) throws IOException {
        try {
            this.minioClient.putObject(
                    PutObjectArgs.builder().bucket(this.bucket).object(path).stream(inputStream, size, -1).build());
        } catch (Exception e) {
            log.error("put object fails", e);
            throw new IOException(e);
        }
    }

    @Override
    public void put(String path, InputStream inputStream) throws IOException {
        try {
            this.minioClient.putObject(
                    PutObjectArgs.builder()
                            .bucket(this.bucket)
                            .object(path)
                            .stream(inputStream, -1, this.partSize)
                            .build());
        } catch (Exception e) {
            log.error("put object fails", e);
            throw new IOException(e);
        }
    }

    @Override
    public void put(String path, byte[] body) throws IOException {
        try {
            this.minioClient.putObject(
                    PutObjectArgs.builder().bucket(this.bucket).object(path)
                            .stream(new ByteArrayInputStream(body), body.length, -1).build());
        } catch (Exception e) {
            log.error("put object fails", e);
            throw new IOException(e);
        }
    }

    @Override
    public LengthAbleInputStream get(String path) throws IOException {
        StorageObjectInfo objectInfo = this.head(path);
        try {
            GetObjectResponse resp = this.minioClient.getObject(GetObjectArgs.builder()
                    .bucket(this.bucket)
                    .object(path)
                    .build());
            return new LengthAbleInputStream(resp, objectInfo.getContentLength());
        } catch (Exception e) {
            log.error("get object fails", e);
            throw new IOException(e);
        }
    }

    @Override
    public LengthAbleInputStream get(String path, Long offset, Long size) throws IOException {
        try {
            GetObjectResponse resp = this.minioClient.getObject(GetObjectArgs.builder()
                    .bucket(this.bucket)
                    .offset(offset)
                    .length(size)
                    .object(path)
                    .build());
            return new LengthAbleInputStream(resp, size);
        } catch (Exception e) {
            log.error("get object fails", e);
            throw new IOException(e);
        }
    }

    @Override
    public Stream<String> list(String path) throws IOException {
        var resp = this.minioClient.listObjects(
                ListObjectsArgs.builder().bucket(this.bucket).prefix(path).recursive(true).build());
        List<String> ret = new ArrayList<>();
        var it = resp.iterator();
        while (it.hasNext()) {
            try {
                ret.add(it.next().get().objectName());
            } catch (Exception e) {
                log.error("list object fails", e);
                throw new IOException(e);
            }
        }
        return ret.stream();
    }

    @Override
    public void delete(String path) throws IOException {
        try {
            this.minioClient.removeObject(RemoveObjectArgs.builder().bucket(this.bucket).object(path).build());
        } catch (Exception e) {
            log.error("delete object fails", e);
            throw new IOException(e);
        }
    }

    @Override
    public String signedUrl(String path, Long expTimeMillis) throws IOException {
        return this.signUrl(path, expTimeMillis, Method.GET);
    }

    @Override
    public String signedPutUrl(String path, Long expTimeMillis) throws IOException {
        return this.signUrl(path, expTimeMillis, Method.PUT);
    }

    private String signUrl(String path, Long expTimeMillis, Method method) throws IOException {
        GetPresignedObjectUrlArgs request = GetPresignedObjectUrlArgs.builder().expiry(expTimeMillis.intValue(),
                TimeUnit.MILLISECONDS).bucket(this.bucket).object(path).method(method).build();
        try {
            return minioClient.getPresignedObjectUrl(request);
        } catch (MinioException | NoSuchAlgorithmException | InvalidKeyException e) {
            log.error("sin url for object {} fails", path, e);
            throw new IOException(e);
        }
    }
}
