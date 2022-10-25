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
import ai.starwhale.mlops.storage.StorageAccessService;
import ai.starwhale.mlops.storage.StorageObjectInfo;
import ai.starwhale.mlops.storage.s3.S3Config;
import ai.starwhale.mlops.storage.util.MetaHelper;
import com.aliyun.oss.OSS;
import com.aliyun.oss.OSSClientBuilder;
import com.aliyun.oss.OSSErrorCode;
import com.aliyun.oss.OSSException;
import com.aliyun.oss.model.GetObjectRequest;
import com.aliyun.oss.model.HeadObjectRequest;
import com.aliyun.oss.model.ListObjectsRequest;
import com.aliyun.oss.model.OSSObjectSummary;
import com.aliyun.oss.model.ObjectListing;
import com.aliyun.oss.model.ObjectMetadata;
import com.google.common.collect.Streams;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Date;
import java.util.stream.Stream;

public class StorageAccessServiceAliyun implements StorageAccessService {

    final String bucket;

    final OSS ossClient;

    public StorageAccessServiceAliyun(S3Config s3Config) {
        this.bucket = s3Config.getBucket();
        this.ossClient = new OSSClientBuilder()
                .build(s3Config.getEndpoint(), s3Config.getAccessKey(), s3Config.getSecretKey());
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
        this.ossClient.putObject(this.bucket, path, inputStream);
    }

    @Override
    public void put(String path, byte[] body) throws IOException {
        this.ossClient.putObject(this.bucket, path, new ByteArrayInputStream(body));
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
}
