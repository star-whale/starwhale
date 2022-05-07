/**
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

import ai.starwhale.mlops.storage.StorageAccessService;
import ai.starwhale.mlops.storage.StorageObjectInfo;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.util.Map;
import java.util.stream.Stream;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.S3ClientBuilder;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.HeadObjectRequest;
import software.amazon.awssdk.services.s3.model.HeadObjectResponse;
import software.amazon.awssdk.services.s3.model.ListObjectsRequest;
import software.amazon.awssdk.services.s3.model.ListObjectsResponse;
import software.amazon.awssdk.services.s3.model.NoSuchKeyException;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.model.S3Object;

public class StorageAccessServiceS3 implements StorageAccessService {

    final S3Config s3Config;

    final S3Client s3client;

    public StorageAccessServiceS3(S3Config s3Config){
        this.s3Config = s3Config;
        AwsBasicCredentials awsCreds = AwsBasicCredentials.create(
            s3Config.getAccessKey(),
            s3Config.getSecretKey());
        S3ClientBuilder s3ClientBuilder = S3Client.builder()
            .credentialsProvider(StaticCredentialsProvider.create(awsCreds))
            .region(Region.of(s3Config.getRegion()));
        if(s3Config.overWriteEndPoint()){
            s3ClientBuilder.endpointOverride(URI.create(s3Config.getEndpoint()));
        }
        this.s3client = s3ClientBuilder
            .build();
    }

    @Override
    public StorageObjectInfo head(String path) throws IOException {
        HeadObjectRequest build = HeadObjectRequest.builder().bucket(s3Config.getBucket()).key(path).build();
        try{
            HeadObjectResponse headObjectResponse = s3client.headObject(build);
            return new StorageObjectInfo(true,headObjectResponse.contentLength(),mapToString(headObjectResponse.metadata()));
        }catch (NoSuchKeyException e){
            return new StorageObjectInfo(false,0L,null);
        }

    }

    private String mapToString(Map<String, String> metadata) {
        if(metadata == null || metadata.isEmpty()){
            return null;
        }
        StringBuilder stringBuilder = new StringBuilder();
        metadata.forEach((k,v)->{
            stringBuilder.append(k);
            stringBuilder.append(":");
            stringBuilder.append(v);
            stringBuilder.append("\n");
        });
        return stringBuilder.toString();
    }

    @Override
    public void put(String path,InputStream inputStream) throws IOException {
        s3client.putObject(PutObjectRequest.builder().bucket(s3Config.getBucket()).key(path).build(),RequestBody.fromInputStream(inputStream,inputStream.available()));
    }

    @Override
    public void put(String path, byte[] body) {
        s3client.putObject(PutObjectRequest.builder().bucket(s3Config.getBucket()).key(path).build(),RequestBody.fromBytes(body));
    }

    @Override
    public InputStream get(String path) {
        return s3client
            .getObject(GetObjectRequest.builder().bucket(s3Config.getBucket()).key(path).build());
    }

    @Override
    public Stream<String> list(String path) {
        final ListObjectsResponse listObjectsResponse = s3client.listObjects(
            ListObjectsRequest.builder().bucket(s3Config.getBucket()).prefix(path).build());
        return listObjectsResponse.contents().stream().map(S3Object::key);
    }

    @Override
    public void delete(String path) throws IOException {
        s3client.deleteObject(DeleteObjectRequest.builder()
            .bucket(s3Config.getBucket())
            .key(path)
            .build());
    }
}
