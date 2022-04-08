/*
 * Copyright 2022.1-2022
 * StarWhale.ai All right reserved. This software is the confidential and proprietary information of
 * StarWhale.ai ("Confidential Information"). You shall not disclose such Confidential Information and shall use it only
 * in accordance with the terms of the license agreement you entered into with StarWhale.ai.
 */

package ai.starwhale.mlops.storage.s3;

import ai.starwhale.mlops.storage.StorageAccessService;
import java.io.IOException;
import java.io.InputStream;
import java.util.stream.Stream;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.S3Configuration;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.ListObjectsRequest;
import software.amazon.awssdk.services.s3.model.ListObjectsResponse;
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
        this.s3client = S3Client.builder()
            .credentialsProvider(StaticCredentialsProvider.create(awsCreds))
            .region(Region.of(s3Config.getRegion()))
            .build();
    }

    @Override
    public void put(String path,InputStream inputStream) {
        s3client.putObject(PutObjectRequest.builder().bucket(s3Config.getBucket()).key(path).build(),RequestBody.fromInputStream(inputStream,(Long)null));
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
