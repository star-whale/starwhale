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

package ai.starwhale.mlops.storage.baidu;

import static org.apache.http.HttpStatus.SC_NOT_FOUND;

import ai.starwhale.mlops.storage.LengthAbleInputStream;
import ai.starwhale.mlops.storage.StorageAccessService;
import ai.starwhale.mlops.storage.StorageObjectInfo;
import ai.starwhale.mlops.storage.s3.S3Config;
import ai.starwhale.mlops.storage.util.MetaHelper;
import com.baidubce.BceServiceException;
import com.baidubce.auth.DefaultBceCredentials;
import com.baidubce.http.HttpMethodName;
import com.baidubce.services.bos.BosClient;
import com.baidubce.services.bos.BosClientConfiguration;
import com.baidubce.services.bos.model.AbortMultipartUploadRequest;
import com.baidubce.services.bos.model.BosObjectSummary;
import com.baidubce.services.bos.model.CompleteMultipartUploadRequest;
import com.baidubce.services.bos.model.GeneratePresignedUrlRequest;
import com.baidubce.services.bos.model.GetObjectRequest;
import com.baidubce.services.bos.model.InitiateMultipartUploadRequest;
import com.baidubce.services.bos.model.ListObjectsRequest;
import com.baidubce.services.bos.model.ListObjectsResponse;
import com.baidubce.services.bos.model.ObjectMetadata;
import com.baidubce.services.bos.model.PartETag;
import com.baidubce.services.bos.model.UploadPartRequest;
import com.google.common.collect.Streams;
import java.io.ByteArrayInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Set;
import java.util.stream.Stream;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class StorageAccessServiceBos implements StorageAccessService {

    private final String bucket;

    private final long partSize;

    private final BosClient bosClient;

    private final Set<String> notFoundErrorCodes = Set.of("NoSuchKey", "NoSuchUpload", "NoSuchBucket", "NoSuchVersion");

    public StorageAccessServiceBos(S3Config s3Config) {
        this.bucket = s3Config.getBucket();
        this.partSize = s3Config.getHugeFilePartSize();

        var config = new BosClientConfiguration();
        config.setCredentials(new DefaultBceCredentials(s3Config.getAccessKey(), s3Config.getSecretKey()));
        if (s3Config.getEndpoint() != null) {
            config.setEndpoint(s3Config.getEndpoint());
            config.setCnameEnabled(true);
        }
        this.bosClient = new BosClient(config);
    }

    private boolean isNotFount(BceServiceException e) {
        // https://cloud.baidu.com/doc/BOS/s/Ajwvysfpl
        return e.getStatusCode() == SC_NOT_FOUND || notFoundErrorCodes.contains(e.getErrorCode());
    }

    @Override
    public StorageObjectInfo head(String path) throws IOException {
        return this.head(path, false);
    }

    @Override
    public StorageObjectInfo head(String path, boolean md5sum) throws IOException {
        try {
            var resp = this.bosClient.getObjectMetadata(this.bucket, path);
            return new StorageObjectInfo(
                    true,
                    resp.getContentLength(),
                    md5sum ? resp.getETag().replace("\"", "") : null,
                    MetaHelper.mapToString(resp.getUserMetadata())
            );
        } catch (BceServiceException e) {
            if (isNotFount(e)) {
                return new StorageObjectInfo(false, 0L, null, null);
            }
            throw new IOException(e);
        }
    }

    @Override
    public void put(String path, InputStream inputStream, long size) throws IOException {
        var metadata = new ObjectMetadata();
        metadata.setContentLength(size);
        this.bosClient.putObject(this.bucket, path, inputStream, metadata);
    }

    @Override
    public void put(String path, byte[] body) throws IOException {
        this.put(path, new ByteArrayInputStream(body), body.length);
    }

    @Override
    public void put(String path, InputStream inputStream) throws IOException {
        var initReq = new InitiateMultipartUploadRequest(this.bucket, path);
        var uploadId = this.bosClient.initiateMultipartUpload(initReq).getUploadId();
        var parts = new ArrayList<PartETag>();
        try {
            for (int i = 1; ; i++) {
                var data = inputStream.readNBytes((int) this.partSize);
                if (data.length == 0) {
                    break;
                }

                var partReq = new UploadPartRequest()
                        .withBucketName(this.bucket)
                        .withKey(path)
                        .withUploadId(uploadId)
                        .withInputStream(new ByteArrayInputStream(data))
                        .withPartSize(data.length)
                        .withPartNumber(i);
                var partResp = this.bosClient.uploadPart(partReq);
                parts.add(partResp.getPartETag());
                if (data.length < this.partSize) {
                    break;
                }
            }

            var req = new CompleteMultipartUploadRequest(this.bucket, path, uploadId, parts);
            this.bosClient.completeMultipartUpload(req);
        } catch (Throwable t) {
            var req = new AbortMultipartUploadRequest(this.bucket, path, uploadId);
            this.bosClient.abortMultipartUpload(req);
            throw new IOException(t);
        }
    }

    @Override
    public LengthAbleInputStream get(String path) throws IOException {
        try {
            var resp = this.bosClient.getObject(this.bucket, path);
            return new LengthAbleInputStream(resp.getObjectContent(), resp.getObjectMetadata().getContentLength());
        } catch (BceServiceException e) {
            if (isNotFount(e)) {
                throw new FileNotFoundException(path);
            }
            log.error("get object fails", e);
            throw new IOException(e);
        }
    }

    @Override
    public LengthAbleInputStream get(String path, Long offset, Long size) throws IOException {
        try {
            var req = new GetObjectRequest(bucket, path).withRange(offset, offset + size - 1);
            var resp = this.bosClient.getObject(req);
            return new LengthAbleInputStream(resp.getObjectContent(), resp.getObjectMetadata().getContentLength());
        } catch (BceServiceException e) {
            if (isNotFount(e)) {
                throw new FileNotFoundException(path);
            }
            log.error("get object fails", e);
            throw new IOException(e);
        }
    }

    @Override
    public Stream<String> list(String path) throws IOException {
        Stream<String> files = Stream.empty();
        try {
            var req = new ListObjectsRequest(this.bucket, path);

            ListObjectsResponse resp;
            do {
                resp = this.bosClient.listObjects(req);
                files = Streams.concat(files, resp.getContents().stream().map(BosObjectSummary::getKey));
                req.setMarker(resp.getNextMarker());
            } while (resp.isTruncated());
            return files;
        } catch (BceServiceException e) {
            if (isNotFount(e)) {
                return files;
            }
            throw e;
        }
    }

    @Override
    public void delete(String path) throws IOException {
        this.bosClient.deleteObject(this.bucket, path);
    }

    @Override
    public String signedUrl(String path, Long expTimeMillis) {
        // -1 means never expired
        int expirationInSeconds = -1;
        if (expTimeMillis != null) {
            expirationInSeconds = (int) (expTimeMillis / 1000);
        }
        return bosClient.generatePresignedUrl(this.bucket, path, expirationInSeconds).toString();
    }

    @Override
    public String signedPutUrl(String path, String contentType, Long expTimeMillis) throws IOException {
        var request = new GeneratePresignedUrlRequest(this.bucket, path, HttpMethodName.PUT);
        int expirationInSeconds = -1;
        if (expTimeMillis != null) {
            expirationInSeconds = (int) (expTimeMillis / 1000);
        }
        request.setExpiration(expirationInSeconds);
        request.setContentType(contentType);
        return bosClient.generatePresignedUrl(request).toString();
    }
}
