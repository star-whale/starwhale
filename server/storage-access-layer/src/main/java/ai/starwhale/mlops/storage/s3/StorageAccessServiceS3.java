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

import static ai.starwhale.mlops.storage.s3.S3Config.endpointToUrl;

import ai.starwhale.mlops.storage.LengthAbleInputStream;
import ai.starwhale.mlops.storage.S3LikeStorageAccessService;
import ai.starwhale.mlops.storage.StorageObjectInfo;
import ai.starwhale.mlops.storage.util.MetaHelper;
import com.google.common.collect.Streams;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.compress.utils.BoundedInputStream;
import org.springframework.util.CollectionUtils;
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
public class StorageAccessServiceS3 extends S3LikeStorageAccessService {

    final S3Client s3client;

    final S3Presigner s3Presigner;
    final List<S3Presigner> s3PresignerEquivalents;

    public StorageAccessServiceS3(S3Config s3Config) {
        super(s3Config);
        AwsBasicCredentials awsCreds = AwsBasicCredentials.create(s3Config.getAccessKey(), s3Config.getSecretKey());
        S3ClientBuilder s3ClientBuilder = S3Client.builder()
                .credentialsProvider(StaticCredentialsProvider.create(awsCreds));
        Builder s3PresignerBuilder = DefaultS3Presigner.builder()
                .credentialsProvider(StaticCredentialsProvider.create(awsCreds));
        if (s3Config.getRegion() != null) {
            var region = Region.of(s3Config.getRegion());
            s3ClientBuilder.region(region);
            s3PresignerBuilder.region(region);
        }
        if (s3Config.getEndpoint() != null) {
            URI uri;
            try {
                uri = s3Config.getEndpointUrl().toURI();
            } catch (URISyntaxException e) {
                throw new RuntimeException(e);
            }
            s3ClientBuilder.endpointOverride(uri);
            s3PresignerBuilder.endpointOverride(uri);
        }
        this.s3client = s3ClientBuilder.build();
        this.s3Presigner = s3PresignerBuilder.build();
        if (!CollectionUtils.isEmpty(s3Config.getEndpointEquivalents())) {
            this.s3PresignerEquivalents = s3Config.getEndpointEquivalents().stream().map(edp -> {
                URI uri;
                try {
                    uri = endpointToUrl(edp).toURI();
                } catch (URISyntaxException e) {
                    log.warn("uri syntax error endpoint equivalent {}", edp);
                    return null;
                } catch (MalformedURLException e) {
                    log.warn("malformated endpoint equivalent {}", edp);
                    return null;
                }
                s3PresignerBuilder.endpointOverride(uri);
                return s3PresignerBuilder.build();
            }).filter(Objects::nonNull).collect(Collectors.toList());
        } else {
            this.s3PresignerEquivalents = List.of();
        }
    }

    @Override
    public StorageObjectInfo head(String path) throws IOException {
        return this.head(path, false);
    }

    @Override
    public StorageObjectInfo head(String path, boolean md5sum) throws IOException {
        HeadObjectRequest build = HeadObjectRequest.builder().bucket(this.bucket).key(path)
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
        if (this.hugeFileThreshold <= 0 || size < this.hugeFileThreshold || this.partSize <= 0) {
            s3client.putObject(
                    PutObjectRequest.builder().bucket(this.bucket).key(path).build(),
                    RequestBody.fromInputStream(inputStream, size));
            return;
        }
        var uploadId = this.s3client.createMultipartUpload(CreateMultipartUploadRequest.builder()
                        .bucket(this.bucket)
                        .key(path)
                        .build())
                .uploadId();
        try {
            var etagList = new ArrayList<String>();
            for (int i = 1; size > 0; ++i) {
                var partSize = Math.min(size, this.partSize);
                var resp = this.s3client.uploadPart(UploadPartRequest.builder()
                                .bucket(this.bucket)
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
                    .bucket(this.bucket)
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
                    .bucket(this.bucket)
                    .key(path)
                    .uploadId(uploadId)
                    .build());
            throw new IOException(t);
        }
    }

    @Override
    public void put(String path, InputStream inputStream) throws IOException {
        var uploadId = this.s3client.createMultipartUpload(CreateMultipartUploadRequest.builder()
                        .bucket(this.bucket)
                        .key(path)
                        .build())
                .uploadId();
        try {
            var etagList = new ArrayList<String>();
            for (int i = 1; ; ++i) {
                var data = inputStream.readNBytes((int) this.partSize);
                if (data.length == 0) {
                    break;
                }
                var resp = this.s3client.uploadPart(UploadPartRequest.builder()
                                .bucket(this.bucket)
                                .key(path)
                                .uploadId(uploadId)
                                .partNumber(i)
                                .contentLength((long) data.length)
                                .build(),
                        RequestBody.fromBytes(data));
                etagList.add(resp.eTag());
                if (data.length < this.partSize) {
                    break;
                }
            }
            this.s3client.completeMultipartUpload(CompleteMultipartUploadRequest.builder()
                    .bucket(this.bucket)
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
                    .bucket(this.bucket)
                    .key(path)
                    .uploadId(uploadId)
                    .build());
            throw new IOException(t);
        }
    }

    @Override
    public void put(String path, byte[] body) {
        s3client.putObject(
                PutObjectRequest.builder().bucket(this.bucket).key(path).build(),
                RequestBody.fromBytes(body));
    }

    @Override
    public LengthAbleInputStream get(String path) throws IOException {
        try {
            var req = GetObjectRequest.builder().bucket(this.bucket).key(path).build();
            var resp = s3client.getObject(req);
            return new LengthAbleInputStream(resp, resp.response().contentLength());
        } catch (NoSuchKeyException e) {
            throw new FileNotFoundException(path);
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
                    .bucket(this.bucket)
                    .key(path)
                    .build();
            var resp = s3client.getObject(req);
            return new LengthAbleInputStream(resp, resp.response().contentLength());
        } catch (NoSuchKeyException e) {
            throw new FileNotFoundException(path);
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
            var reqBuilder = ListObjectsRequest.builder().bucket(this.bucket).prefix(path);
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
                .bucket(this.bucket)
                .key(path)
                .build());
    }

    @Override
    public String signedUrl(String path, Long expTimeMillis) {
        return signGetUrl(path, expTimeMillis, this.s3Presigner);
    }


    @Override
    public List<String> signedUrlAllDomains(String path, Long expTimeMillis) {
        return Stream.concat(Stream.of(s3Presigner), s3PresignerEquivalents.stream())
                .map(singer -> signGetUrl(
                        path,
                        expTimeMillis,
                        singer
                )).collect(Collectors.toList());
    }

    @Override
    public String signedPutUrl(String path, String contentType, Long expTimeMillis) {
        return signPutUrl(path, contentType, expTimeMillis, this.s3Presigner);
    }

    @Override
    public List<String> signedPutUrlAllDomains(String path, String contentType, Long expTimeMillis) {
        return Stream.concat(Stream.of(s3Presigner), s3PresignerEquivalents.stream())
                .map(singer -> signPutUrl(
                        path,
                        contentType,
                        expTimeMillis,
                        singer
                )).collect(Collectors.toList());
    }


    private String signGetUrl(String path, Long expTimeMillis, S3Presigner signer) {
        return signer.presignGetObject(GetObjectPresignRequest.builder()
                                               .getObjectRequest(GetObjectRequest.builder()
                                                                         .bucket(this.bucket)
                                                                         .key(path)
                                                                         .build())
                                               .signatureDuration(Duration.ofMillis(expTimeMillis))
                                               .build()).url().toString();
    }

    private String signPutUrl(String path, String contentType, Long expTimeMillis, S3Presigner signer) {
        return signer.presignPutObject(PutObjectPresignRequest.builder()
                                               .putObjectRequest(PutObjectRequest.builder()
                                                                         .bucket(this.bucket)
                                                                         .key(path)
                                                                         .contentType(contentType)
                                                                         .build())
                                               .signatureDuration(Duration.ofMillis(expTimeMillis))
                                               .build()).url().toString();
    }

}
