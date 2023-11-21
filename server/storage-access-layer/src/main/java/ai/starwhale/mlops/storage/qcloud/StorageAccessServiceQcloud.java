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

package ai.starwhale.mlops.storage.qcloud;

import static org.apache.http.HttpStatus.SC_NOT_FOUND;

import ai.starwhale.mlops.storage.LengthAbleInputStream;
import ai.starwhale.mlops.storage.S3LikeStorageAccessService;
import ai.starwhale.mlops.storage.StorageObjectInfo;
import ai.starwhale.mlops.storage.s3.S3Config;
import ai.starwhale.mlops.storage.util.MetaHelper;
import com.google.common.collect.Streams;
import com.qcloud.cos.COSClient;
import com.qcloud.cos.ClientConfig;
import com.qcloud.cos.auth.BasicCOSCredentials;
import com.qcloud.cos.endpoint.SuffixEndpointBuilder;
import com.qcloud.cos.exception.CosServiceException;
import com.qcloud.cos.http.HttpMethodName;
import com.qcloud.cos.http.HttpProtocol;
import com.qcloud.cos.model.AbortMultipartUploadRequest;
import com.qcloud.cos.model.COSObjectSummary;
import com.qcloud.cos.model.CompleteMultipartUploadRequest;
import com.qcloud.cos.model.GeneratePresignedUrlRequest;
import com.qcloud.cos.model.GetObjectRequest;
import com.qcloud.cos.model.InitiateMultipartUploadRequest;
import com.qcloud.cos.model.ListObjectsRequest;
import com.qcloud.cos.model.ObjectListing;
import com.qcloud.cos.model.ObjectMetadata;
import com.qcloud.cos.model.PartETag;
import com.qcloud.cos.model.UploadPartRequest;
import com.qcloud.cos.region.Region;
import java.io.ByteArrayInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Date;
import java.util.stream.Stream;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class StorageAccessServiceQcloud extends S3LikeStorageAccessService {


    private final COSClient cosClient;

    public StorageAccessServiceQcloud(S3Config s3Config) {
        super(s3Config);

        // https://cloud.tencent.com/document/product/436/10199
        var cred = new BasicCOSCredentials(s3Config.getAccessKey(), s3Config.getSecretKey());
        var cfg = new ClientConfig();
        if (s3Config.getRegion() != null) {
            cfg.setRegion(new Region(s3Config.getRegion()));
        }
        if (s3Config.getEndpoint() != null) {
            var url = s3Config.getEndpointUrl();
            if (url.getProtocol().equalsIgnoreCase("https")) {
                cfg.setHttpProtocol(HttpProtocol.https);
            } else {
                cfg.setHttpProtocol(HttpProtocol.http);
            }
            cfg.setEndpointBuilder(new SuffixEndpointBuilder(url.getAuthority()));
        }
        this.cosClient = new COSClient(cred, cfg);
    }

    @Override
    public StorageObjectInfo head(String path) throws IOException {
        return this.head(path, false);
    }

    @Override
    public StorageObjectInfo head(String path, boolean md5sum) throws IOException {
        try {
            var resp = this.cosClient.getObjectMetadata(this.bucket, path);
            return new StorageObjectInfo(
                    true,
                    resp.getContentLength(),
                    md5sum ? resp.getETag().replace("\"", "") : null,
                    MetaHelper.mapToString(resp.getUserMetadata())
            );
        } catch (CosServiceException e) {
            // check if the object exists
            // https://cloud.tencent.com/document/product/436/7730
            if (e.getStatusCode() == SC_NOT_FOUND) {
                return new StorageObjectInfo(false, 0L, null, null);
            }
            throw new IOException(e);
        }
    }

    @Override
    public void put(String path, InputStream inputStream, long size) throws IOException {
        var metadata = new ObjectMetadata();
        metadata.setContentLength(size);
        this.cosClient.putObject(this.bucket, path, inputStream, metadata);
    }

    @Override
    public void put(String path, byte[] body) throws IOException {
        this.put(path, new ByteArrayInputStream(body), body.length);
    }

    @Override
    public void put(String path, InputStream inputStream) throws IOException {
        var initReq = new InitiateMultipartUploadRequest(this.bucket, path);
        var uploadId = this.cosClient.initiateMultipartUpload(initReq).getUploadId();
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
                var partResp = this.cosClient.uploadPart(partReq);
                parts.add(partResp.getPartETag());
                if (data.length < this.partSize) {
                    break;
                }
            }

            var req = new CompleteMultipartUploadRequest(this.bucket, path, uploadId, parts);
            this.cosClient.completeMultipartUpload(req);
        } catch (Throwable t) {
            var req = new AbortMultipartUploadRequest(this.bucket, path, uploadId);
            this.cosClient.abortMultipartUpload(req);
            throw new IOException(t);
        }
    }

    @Override
    public LengthAbleInputStream get(String path) throws IOException {
        try {
            var resp = this.cosClient.getObject(this.bucket, path);
            return new LengthAbleInputStream(resp.getObjectContent(), resp.getObjectMetadata().getContentLength());
        } catch (CosServiceException e) {
            if (e.getStatusCode() == SC_NOT_FOUND) {
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
            var resp = this.cosClient.getObject(req);
            return new LengthAbleInputStream(resp.getObjectContent(), resp.getObjectMetadata().getContentLength());
        } catch (CosServiceException e) {
            if (e.getStatusCode() == SC_NOT_FOUND) {
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
            var req = new ListObjectsRequest();
            req.setBucketName(this.bucket);
            req.setPrefix(path);
            req.setMaxKeys(1000);

            ObjectListing resp;
            do {
                resp = this.cosClient.listObjects(req);
                files = Streams.concat(files, resp.getObjectSummaries().stream().map(COSObjectSummary::getKey));
                req.setMarker(resp.getNextMarker());
            } while (resp.isTruncated());
            return files;
        } catch (CosServiceException e) {
            if (e.getStatusCode() == SC_NOT_FOUND) {
                return files;
            }
            throw e;
        }
    }

    @Override
    public void delete(String path) throws IOException {
        this.cosClient.deleteObject(this.bucket, path);
    }

    @Override
    public String signedUrl(String path, Long expTimeMillis) {
        var expiration = new Date(System.currentTimeMillis() + expTimeMillis);
        return cosClient.generatePresignedUrl(this.bucket, path, expiration).toString();
    }

    @Override
    public String signedPutUrl(String path, String contentType, Long expTimeMillis) throws IOException {
        var request = new GeneratePresignedUrlRequest(this.bucket, path, HttpMethodName.PUT);
        request.setExpiration(new Date(System.currentTimeMillis() + expTimeMillis));
        request.setContentType(contentType);
        return cosClient.generatePresignedUrl(request).toString();
    }
}
