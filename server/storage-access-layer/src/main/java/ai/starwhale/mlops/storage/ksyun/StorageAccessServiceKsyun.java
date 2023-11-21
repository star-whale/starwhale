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

package ai.starwhale.mlops.storage.ksyun;

import ai.starwhale.mlops.storage.LengthAbleInputStream;
import ai.starwhale.mlops.storage.NopCloserInputStream;
import ai.starwhale.mlops.storage.S3LikeStorageAccessService;
import ai.starwhale.mlops.storage.StorageObjectInfo;
import ai.starwhale.mlops.storage.s3.S3Config;
import ai.starwhale.mlops.storage.util.MetaHelper;
import com.google.common.collect.Streams;
import com.ksyun.ks3.dto.Ks3ObjectSummary;
import com.ksyun.ks3.dto.ObjectListing;
import com.ksyun.ks3.dto.ObjectMetadata;
import com.ksyun.ks3.dto.PartETag;
import com.ksyun.ks3.exception.Ks3ServiceException;
import com.ksyun.ks3.exception.serviceside.NoSuchKeyException;
import com.ksyun.ks3.exception.serviceside.NotFoundException;
import com.ksyun.ks3.http.HttpMethod;
import com.ksyun.ks3.http.Region;
import com.ksyun.ks3.service.Ks3Client;
import com.ksyun.ks3.service.Ks3ClientConfig;
import com.ksyun.ks3.service.Ks3ClientConfig.PROTOCOL;
import com.ksyun.ks3.service.request.AbortMultipartUploadRequest;
import com.ksyun.ks3.service.request.CompleteMultipartUploadRequest;
import com.ksyun.ks3.service.request.GeneratePresignedUrlRequest;
import com.ksyun.ks3.service.request.GetObjectRequest;
import com.ksyun.ks3.service.request.HeadObjectRequest;
import com.ksyun.ks3.service.request.InitiateMultipartUploadRequest;
import com.ksyun.ks3.service.request.ListObjectsRequest;
import com.ksyun.ks3.service.request.UploadPartRequest;
import java.io.ByteArrayInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Date;
import java.util.stream.Stream;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class StorageAccessServiceKsyun extends S3LikeStorageAccessService {

    private final Ks3Client ks3Client;

    public StorageAccessServiceKsyun(S3Config s3Config) {
        super(s3Config);
        Ks3ClientConfig config = new Ks3ClientConfig();
        if (s3Config.getRegion() != null) {
            config.setRegion(Region.valueOf(s3Config.getRegion().toUpperCase()));
        }
        if (s3Config.getEndpoint() != null) {
            var url = s3Config.getEndpointUrl();
            if (url.getProtocol().equalsIgnoreCase("https")) {
                config.setProtocol(PROTOCOL.https);
            } else {
                config.setProtocol(PROTOCOL.http);
            }
            config.setEndpoint(url.getAuthority());
        }
        config.setPathStyleAccess(false);
        this.ks3Client = new Ks3Client(s3Config.getAccessKey(), s3Config.getSecretKey(), config);
    }

    @Override
    public StorageObjectInfo head(String path) throws IOException {
        return this.head(path, false);
    }

    @Override
    public StorageObjectInfo head(String path, boolean md5sum) throws IOException {
        try {
            var resp = this.ks3Client.headObject(new HeadObjectRequest(this.bucket, path));
            return new StorageObjectInfo(true,
                    resp.getObjectMetadata().getContentLength(),
                    md5sum ? resp.getObjectMetadata().getETag().replace("\"", "") : null,
                    MetaHelper.mapToString(resp.getObjectMetadata().getAllUserMeta()));
        } catch (NotFoundException e) {
            return new StorageObjectInfo(false, 0L, null, null);
        }
    }

    @Override
    public void put(String path, InputStream inputStream, long size) throws IOException {
        var meta = new ObjectMetadata();
        meta.setContentLength(size);
        var is = new NopCloserInputStream(inputStream);
        this.ks3Client.putObject(this.bucket, path, is, meta);
    }

    @Override
    public void put(String path, byte[] body) throws IOException {
        var meta = new ObjectMetadata();
        meta.setContentLength(body.length);
        this.ks3Client.putObject(this.bucket, path, new ByteArrayInputStream(body), meta);
    }

    @Override
    public void put(String path, InputStream inputStream) throws IOException {
        var uploadId = this.ks3Client.initiateMultipartUpload(new InitiateMultipartUploadRequest(this.bucket, path))
                .getUploadId();
        try {
            var etagList = new ArrayList<PartETag>();
            for (int i = 1; ; ++i) {
                var data = inputStream.readNBytes((int) this.partSize);
                if (data.length == 0) {
                    break;
                }
                var resp = this.ks3Client.uploadPart(new UploadPartRequest(
                        this.bucket,
                        path,
                        uploadId,
                        i,
                        new ByteArrayInputStream(data),
                        data.length));
                etagList.add(resp);
                if (data.length < this.partSize) {
                    break;
                }
            }
            this.ks3Client.completeMultipartUpload(
                    new CompleteMultipartUploadRequest(this.bucket, path, uploadId, etagList));
        } catch (Throwable t) {
            this.ks3Client.abortMultipartUpload(new AbortMultipartUploadRequest(this.bucket, path, uploadId));
            throw new IOException(t);
        }
    }

    @Override
    public LengthAbleInputStream get(String path) throws IOException {
        try {
            var resp = this.ks3Client.getObject(this.bucket, path);
            return new LengthAbleInputStream(resp.getObject().getObjectContent(),
                    resp.getObject().getObjectMetadata().getContentLength());
        } catch (Ks3ServiceException e) {
            if (e instanceof NoSuchKeyException) {
                throw new FileNotFoundException(path);
            }
            log.error("get object fails", e);
            throw new IOException(e);
        }
    }

    @Override
    public LengthAbleInputStream get(String path, Long offset, Long size) throws IOException {
        try {
            var req = new GetObjectRequest(bucket, path);
            req.setRange(offset, offset + size - 1);
            var resp = this.ks3Client.getObject(req);
            return new LengthAbleInputStream(resp.getObject().getObjectContent(),
                    resp.getObject().getObjectMetadata().getContentLength());
        } catch (Ks3ServiceException e) {
            if (e instanceof NoSuchKeyException) {
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
            var req = new ListObjectsRequest(this.bucket);
            req.setPrefix(path);
            ObjectListing resp;
            do {
                resp = this.ks3Client.listObjects(req);
                files = Streams.concat(files, resp.getObjectSummaries().stream().map(Ks3ObjectSummary::getKey));
                req.setMarker(resp.getNextMarker());
            } while (resp.isTruncated());
            return files;
        } catch (NotFoundException e) {
            return files;
        }
    }

    @Override
    public void delete(String path) throws IOException {
        this.ks3Client.deleteObject(this.bucket, path);
    }

    @Override
    public String signedUrl(String path, Long expTimeMillis) {
        return ks3Client.generatePresignedUrl(this.bucket, path,
                (int) ((System.currentTimeMillis() + expTimeMillis) / 1000));
    }

    @Override
    public String signedPutUrl(String path, String contentType, Long expTimeMillis) throws IOException {
        var request = new GeneratePresignedUrlRequest();
        request.setBucket(this.bucket);
        request.setKey(path);
        request.setMethod(HttpMethod.PUT);
        request.setExpiration(new Date(System.currentTimeMillis() + expTimeMillis));
        request.setContentType(contentType);
        return ks3Client.generatePresignedUrl(request);
    }
}
