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

package ai.starwhale.mlops.storage.aliyun;

import ai.starwhale.mlops.storage.LengthAbleInputStream;
import ai.starwhale.mlops.storage.NopCloserInputStream;
import ai.starwhale.mlops.storage.StorageAccessService;
import ai.starwhale.mlops.storage.StorageObjectInfo;
import ai.starwhale.mlops.storage.s3.S3Config;
import ai.starwhale.mlops.storage.util.MetaHelper;
import com.aliyun.oss.ClientBuilderConfiguration;
import com.aliyun.oss.HttpMethod;
import com.aliyun.oss.OSS;
import com.aliyun.oss.OSSClientBuilder;
import com.aliyun.oss.OSSErrorCode;
import com.aliyun.oss.OSSException;
import com.aliyun.oss.model.AbortMultipartUploadRequest;
import com.aliyun.oss.model.CompleteMultipartUploadRequest;
import com.aliyun.oss.model.GetObjectRequest;
import com.aliyun.oss.model.HeadObjectRequest;
import com.aliyun.oss.model.InitiateMultipartUploadRequest;
import com.aliyun.oss.model.ListObjectsRequest;
import com.aliyun.oss.model.OSSObjectSummary;
import com.aliyun.oss.model.ObjectListing;
import com.aliyun.oss.model.ObjectMetadata;
import com.aliyun.oss.model.PartETag;
import com.aliyun.oss.model.UploadPartRequest;
import com.google.common.collect.Streams;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Date;
import java.util.stream.Stream;

public class StorageAccessServiceAliyun implements StorageAccessService {

    private final String bucket;

    private final long partSize;

    private final OSS ossClient;

    public StorageAccessServiceAliyun(S3Config s3Config) {
        this.bucket = s3Config.getBucket();
        this.partSize = s3Config.getHugeFilePartSize();

        var config = new ClientBuilderConfiguration();
        config.setRequestTimeoutEnabled(true);
        this.ossClient = new OSSClientBuilder()
                .build(s3Config.getEndpoint(), s3Config.getAccessKey(), s3Config.getSecretKey(), config);
    }

    @Override
    public StorageObjectInfo head(String path) throws IOException {
        try {
            var resp = this.ossClient.headObject(new HeadObjectRequest(this.bucket, path));
            return new StorageObjectInfo(true, resp.getContentLength(), MetaHelper.mapToString(resp.getUserMetadata()));
        } catch (OSSException e) {
            if (e.getErrorCode().equals(OSSErrorCode.NO_SUCH_KEY)) {
                return new StorageObjectInfo(false, 0L, null);
            }
            throw e;
        }
    }

    @Override
    public void put(String path, InputStream inputStream, long size) throws IOException {
        var meta = new ObjectMetadata();
        meta.setContentLength(size);
        // aliyun oss sdk will close the original input stream at the end
        // https://github.com/aliyun/aliyun-oss-java-sdk/blob/10727ab9f79efa2a4f2c7fbec348e44c04dd6c42/src/main/java/com/aliyun/oss/common/comm/ServiceClient.java#L89
        var is = new NopCloserInputStream(inputStream);
        this.ossClient.putObject(this.bucket, path, is, meta);
    }

    @Override
    public void put(String path, byte[] body) throws IOException {
        this.ossClient.putObject(this.bucket, path, new ByteArrayInputStream(body));
    }

    @Override
    public void put(String path, InputStream inputStream) throws IOException {
        var uploadId = this.ossClient.initiateMultipartUpload(new InitiateMultipartUploadRequest(this.bucket, path))
                .getUploadId();
        try {
            var etagList = new ArrayList<PartETag>();
            for (int i = 1; ; ++i) {
                var data = inputStream.readNBytes((int) this.partSize);
                if (data.length == 0) {
                    break;
                }
                var resp = this.ossClient.uploadPart(new UploadPartRequest(
                        this.bucket,
                        path,
                        uploadId,
                        i,
                        new ByteArrayInputStream(data),
                        data.length));
                etagList.add(resp.getPartETag());
                if (data.length < this.partSize) {
                    break;
                }
            }
            this.ossClient.completeMultipartUpload(
                    new CompleteMultipartUploadRequest(this.bucket, path, uploadId, etagList));
        } catch (Throwable t) {
            this.ossClient.abortMultipartUpload(new AbortMultipartUploadRequest(this.bucket, path, uploadId));
            throw new IOException(t);
        }
    }

    @Override
    public LengthAbleInputStream get(String path) throws IOException {
        var resp = this.ossClient.getObject(this.bucket, path);
        return new LengthAbleInputStream(resp.getObjectContent(), resp.getObjectMetadata().getContentLength());
    }

    @Override
    public LengthAbleInputStream get(String path, Long offset, Long size) throws IOException {
        var req = new GetObjectRequest(bucket, path).withRange(offset, offset + size - 1);
        var resp = this.ossClient.getObject(req);
        return new LengthAbleInputStream(resp.getObjectContent(), resp.getObjectMetadata().getContentLength());
    }

    @Override
    public Stream<String> list(String path) throws IOException {
        Stream<String> files = Stream.empty();
        try {
            var req = new ListObjectsRequest(this.bucket).withPrefix(path);
            ObjectListing resp;
            do {
                resp = this.ossClient.listObjects(req);
                files = Streams.concat(files, resp.getObjectSummaries().stream().map(OSSObjectSummary::getKey));
                req.setMarker(resp.getNextMarker());
            } while (resp.isTruncated());
            return files;
        } catch (OSSException e) {
            if (e.getErrorCode().equals(OSSErrorCode.NO_SUCH_KEY)) {
                return files;
            }
            throw e;
        }
    }

    @Override
    public void delete(String path) throws IOException {
        this.ossClient.deleteObject(this.bucket, path);
    }

    @Override
    public String signedUrl(String path, Long expTimeMillis) throws IOException {
        return ossClient.generatePresignedUrl(this.bucket, path, new Date(System.currentTimeMillis() + expTimeMillis))
                .toString();
    }

    @Override
    public String signedPutUrl(String path, Long expTimeMillis) throws IOException {
        return ossClient.generatePresignedUrl(this.bucket,
                        path,
                        new Date(System.currentTimeMillis() + expTimeMillis),
                        HttpMethod.PUT)
                .toString();
    }
}
