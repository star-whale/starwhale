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

package ai.starwhale.mlops.storage.s3;

import ai.starwhale.mlops.storage.LengthAbleInputStream;
import ai.starwhale.mlops.storage.StorageAccessService;
import ai.starwhale.mlops.storage.StorageObjectInfo;
import ai.starwhale.mlops.storage.util.MetaHelper;
import com.google.common.collect.Streams;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.time.Duration;
import java.util.ArrayList;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.compress.utils.BoundedInputStream;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.S3ClientBuilder;
import software.amazon.awssdk.services.s3.internal.signing.DefaultS3Presigner;
import software.amazon.awssdk.services.s3.model.AbortMultipartUploadRequest;
import software.amazon.awssdk.services.s3.model.CompleteMultipartUploadRequest;
import software.amazon.awssdk.services.s3.model.CompletedMultipartUpload;
import software.amazon.awssdk.services.s3.model.CompletedPart;
import software.amazon.awssdk.services.s3.model.CreateMultipartUploadRequest;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.HeadObjectRequest;
import software.amazon.awssdk.services.s3.model.HeadObjectResponse;
import software.amazon.awssdk.services.s3.model.ListMultipartUploadsRequest;
import software.amazon.awssdk.services.s3.model.ListObjectsRequest;
import software.amazon.awssdk.services.s3.model.ListObjectsResponse;
import software.amazon.awssdk.services.s3.model.NoSuchKeyException;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.model.S3Object;
import software.amazon.awssdk.services.s3.model.UploadPartRequest;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.S3Presigner.Builder;
import software.amazon.awssdk.services.s3.presigner.model.GetObjectPresignRequest;
import software.amazon.awssdk.services.s3.presigner.model.PutObjectPresignRequest;

@Slf4j
public class StorageAccessServiceS3 implements StorageAccessService {

    final S3Config s3Config;

    final S3Client s3client;

    final S3Presigner s3Presigner;

    public StorageAccessServiceS3(S3Config s3Config) {
        this.s3Config = s3Config;
        AwsBasicCredentials awsCreds = AwsBasicCredentials.create(s3Config.getAccessKey(), s3Config.getSecretKey());
        S3ClientBuilder s3ClientBuilder = S3Client.builder()
                .credentialsProvider(StaticCredentialsProvider.create(awsCreds))
                .region(Region.of(s3Config.getRegion()));
        if (s3Config.overWriteEndPoint()) {
            s3ClientBuilder.endpointOverride(URI.create(s3Config.getEndpoint()));
        }
        this.s3client = s3ClientBuilder.build();
        Builder s3ResignedBuilder = DefaultS3Presigner.builder()
                .credentialsProvider(StaticCredentialsProvider.create(awsCreds))
                .region(Region.of(s3Config.getRegion()));
        if (s3Config.overWriteEndPoint()) {
            s3ResignedBuilder.endpointOverride(URI.create(s3Config.getEndpoint()));
        }
        this.s3Presigner = s3ResignedBuilder.build();

        // abort all previous pending multipart uploads
        var resp = this.s3client.listMultipartUploads(ListMultipartUploadsRequest.builder()
                .bucket(s3Config.getBucket())
                .build());
        for (; ; ) {
            for (var upload : resp.uploads()) {
                this.s3client.abortMultipartUpload(AbortMultipartUploadRequest.builder()
                        .bucket(s3Config.getBucket())
                        .key(upload.key())
                        .uploadId(upload.uploadId())
                        .build());
            }
            if (!resp.isTruncated()) {
                break;
            }
            resp = this.s3client.listMultipartUploads(ListMultipartUploadsRequest.builder()
                    .bucket(s3Config.getBucket())
                    .keyMarker(resp.nextKeyMarker())
                    .uploadIdMarker(resp.nextUploadIdMarker())
                    .build());
        }
    }

    @Override
    public StorageObjectInfo head(String path) throws IOException {
        return this.head(path, false);
    }

    @Override
    public StorageObjectInfo head(String path, boolean md5sum) throws IOException {
        HeadObjectRequest build = HeadObjectRequest.builder().bucket(s3Config.getBucket()).key(path)
                .build();
        try {
            HeadObjectResponse headObjectResponse = s3client.headObject(build);
            return new StorageObjectInfo(true,
                    headObjectResponse.contentLength(),
                    md5sum ? headObjectResponse.eTag().replace("\"", "") : null,
                    MetaHelper.mapToString(headObjectResponse.metadata()));
        } catch (NoSuchKeyException e) {
            return new StorageObjectInfo(false, 0L, null, null);
        }
    }

    @Override
    public void put(String path, InputStream inputStream, long size) throws IOException {
        if (this.s3Config.getHugeFileThreshold() <= 0
                || size < this.s3Config.getHugeFileThreshold()
                || this.s3Config.getHugeFilePartSize() <= 0) {
            s3client.putObject(
                    PutObjectRequest.builder().bucket(s3Config.getBucket()).key(path).build(),
                    RequestBody.fromInputStream(inputStream, size));
            return;
        }
        var uploadId = this.s3client.createMultipartUpload(CreateMultipartUploadRequest.builder()
                        .bucket(this.s3Config.getBucket())
                        .key(path)
                        .build())
                .uploadId();
        try {
            var etagList = new ArrayList<String>();
            for (int i = 1; size > 0; ++i) {
                var partSize = Math.min(size, this.s3Config.getHugeFilePartSize());
                var resp = this.s3client.uploadPart(UploadPartRequest.builder()
                                .bucket(this.s3Config.getBucket())
                                .key(path)
                                .uploadId(uploadId)
                                .partNumber(i)
                                .contentLength(partSize)
                                .build(),
                        RequestBody.fromInputStream(new BoundedInputStream(inputStream, partSize), partSize));
                size -= partSize;
                etagList.add(resp.eTag());
            }
            this.s3client.completeMultipartUpload(CompleteMultipartUploadRequest.builder()
                    .bucket(this.s3Config.getBucket())
                    .key(path)
                    .uploadId(uploadId)
                    .multipartUpload(CompletedMultipartUpload.builder()
                            .parts(IntStream.range(0, etagList.size())
                                    .mapToObj(i -> CompletedPart.builder()
                                            .partNumber(i + 1)
                                            .eTag(etagList.get(i))
                                            .build())
                                    .collect(Collectors.toList()))
                            .build())
                    .build());
        } catch (Throwable t) {
            log.error("multipart file upload aborted", t);
            this.s3client.abortMultipartUpload(AbortMultipartUploadRequest.builder()
                    .bucket(this.s3Config.getBucket())
                    .key(path)
                    .uploadId(uploadId)
                    .build());
            throw new IOException(t);
        }
    }

    @Override
    public void put(String path, InputStream inputStream) throws IOException {
        var uploadId = this.s3client.createMultipartUpload(CreateMultipartUploadRequest.builder()
                        .bucket(this.s3Config.getBucket())
                        .key(path)
                        .build())
                .uploadId();
        try {
            var etagList = new ArrayList<String>();
            for (int i = 1; ; ++i) {
                var data = inputStream.readNBytes((int) this.s3Config.getHugeFilePartSize());
                if (data.length == 0) {
                    break;
                }
                var resp = this.s3client.uploadPart(UploadPartRequest.builder()
                                .bucket(this.s3Config.getBucket())
                                .key(path)
                                .uploadId(uploadId)
                                .partNumber(i)
                                .contentLength((long) data.length)
                                .build(),
                        RequestBody.fromBytes(data));
                etagList.add(resp.eTag());
                if (data.length < this.s3Config.getHugeFilePartSize()) {
                    break;
                }
            }
            this.s3client.completeMultipartUpload(CompleteMultipartUploadRequest.builder()
                    .bucket(this.s3Config.getBucket())
                    .key(path)
                    .uploadId(uploadId)
                    .multipartUpload(CompletedMultipartUpload.builder()
                            .parts(IntStream.range(0, etagList.size())
                                    .mapToObj(i -> CompletedPart.builder()
                                            .partNumber(i + 1)
                                            .eTag(etagList.get(i))
                                            .build())
                                    .collect(Collectors.toList()))
                            .build())
                    .build());
        } catch (Throwable t) {
            log.error("multipart file upload aborted", t);
            this.s3client.abortMultipartUpload(AbortMultipartUploadRequest.builder()
                    .bucket(this.s3Config.getBucket())
                    .key(path)
                    .uploadId(uploadId)
                    .build());
            throw new IOException(t);
        }
    }

    @Override
    public void put(String path, byte[] body) {
        s3client.putObject(
                PutObjectRequest.builder().bucket(s3Config.getBucket()).key(path).build(),
                RequestBody.fromBytes(body));
    }

    @Override
    public LengthAbleInputStream get(String path) throws IOException {
        try {
            var req = GetObjectRequest.builder().bucket(s3Config.getBucket()).key(path).build();
            var resp = s3client.getObject(req);
            return new LengthAbleInputStream(resp, resp.response().contentLength());
        } catch (Exception e) {
            log.error("get object fails", e);
            throw new IOException(e);
        }
    }


    //bytes=0-10098
    static final String RANGE_FORMAT = "bytes=%d-%d";

    @Override
    public LengthAbleInputStream get(String path, Long offset, Long size) throws IOException {
        if (null == offset || null == size || offset < 0 || size <= 0) {
            return get(path);
        }
        try {
            var req = GetObjectRequest.builder()
                    .range(String.format(RANGE_FORMAT, offset, offset + size - 1))
                    .bucket(s3Config.getBucket())
                    .key(path)
                    .build();
            var resp = s3client.getObject(req);
            return new LengthAbleInputStream(resp, resp.response().contentLength());
        } catch (Exception e) {
            log.error("get object fails", e);
            throw new IOException(e);
        }
    }

    @Override
    public Stream<String> list(String path) {
        Stream<String> files = Stream.empty();
        try {
            ListObjectsResponse resp;
            var reqBuilder = ListObjectsRequest.builder().bucket(s3Config.getBucket()).prefix(path);
            do {
                resp = s3client.listObjects(reqBuilder.build());
                files = Streams.concat(files, resp.contents().stream().map(S3Object::key));
                reqBuilder.marker(resp.nextMarker());
            } while (resp.isTruncated());
            return files;
        } catch (NoSuchKeyException e) {
            return files;
        }
    }

    @Override
    public void delete(String path) throws IOException {
        s3client.deleteObject(DeleteObjectRequest.builder()
                .bucket(s3Config.getBucket())
                .key(path)
                .build());
    }

    @Override
    public String signedUrl(String path, Long expTimeMillis) {
        return s3Presigner.presignGetObject(GetObjectPresignRequest.builder()
                .getObjectRequest(GetObjectRequest.builder().bucket(this.s3Config.getBucket()).key(path).build())
                .signatureDuration(Duration.ofMillis(expTimeMillis))
                .build()).url().toString();
    }

    @Override
    public String signedPutUrl(String path, Long expTimeMillis) {
        return s3Presigner.presignPutObject(PutObjectPresignRequest.builder()
                .putObjectRequest(PutObjectRequest.builder().bucket(this.s3Config.getBucket()).key(path).build())
                .signatureDuration(Duration.ofMillis(expTimeMillis))
                .build()).url().toString();
    }
}
