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

package ai.starwhale.mlops.domain.dataset;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import ai.starwhale.mlops.JobMockHolder;
import ai.starwhale.mlops.api.protocol.dataset.upload.DatasetUploadRequest;
import ai.starwhale.mlops.common.IdConverter;
import ai.starwhale.mlops.common.VersionAliasConverter;
import ai.starwhale.mlops.domain.dataset.bo.DatasetVersion;
import ai.starwhale.mlops.domain.dataset.index.datastore.DataStoreTableNameHelper;
import ai.starwhale.mlops.domain.dataset.mapper.DatasetMapper;
import ai.starwhale.mlops.domain.dataset.mapper.DatasetVersionMapper;
import ai.starwhale.mlops.domain.dataset.po.DatasetEntity;
import ai.starwhale.mlops.domain.dataset.po.DatasetVersionEntity;
import ai.starwhale.mlops.domain.dataset.upload.DatasetUploader;
import ai.starwhale.mlops.domain.dataset.upload.DatasetVersionWithMetaConverter;
import ai.starwhale.mlops.domain.dataset.upload.HotDatasetHolder;
import ai.starwhale.mlops.domain.job.bo.Job;
import ai.starwhale.mlops.domain.job.cache.HotJobHolder;
import ai.starwhale.mlops.domain.job.cache.HotJobHolderImpl;
import ai.starwhale.mlops.domain.project.ProjectService;
import ai.starwhale.mlops.domain.project.bo.Project;
import ai.starwhale.mlops.domain.storage.StoragePathCoordinator;
import ai.starwhale.mlops.domain.user.UserService;
import ai.starwhale.mlops.domain.user.bo.User;
import ai.starwhale.mlops.exception.SwValidationException;
import ai.starwhale.mlops.storage.LengthAbleInputStream;
import ai.starwhale.mlops.storage.StorageAccessService;
import ai.starwhale.mlops.storage.StorageObjectInfo;
import io.micrometer.core.instrument.MeterRegistry;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.stream.Stream;
import javax.servlet.ServletOutputStream;
import javax.servlet.WriteListener;
import javax.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.mockito.stubbing.Answer;
import org.springframework.mock.web.MockMultipartFile;

/**
 * test for {@link DatasetUploader}
 */
public class DatasetUploaderTest {

    final DataStoreTableNameHelper dataStoreTableNameHelper = new DataStoreTableNameHelper();

    @Test
    public void testDatasetUploader() throws IOException {
        DatasetVersionWithMetaConverter datasetVersionWithMetaConverter = new DatasetVersionWithMetaConverter();
        HotDatasetHolder hotDatasetHolder = new HotDatasetHolder(datasetVersionWithMetaConverter);
        DatasetMapper datasetMapper = mock(DatasetMapper.class);
        DatasetVersionMapper datasetVersionMapper = mock(DatasetVersionMapper.class);
        given(datasetVersionMapper.insert(any())).will((Answer<Integer>) invocation -> {
            DatasetVersionEntity version = invocation.getArgument(0);
            version.setId(1L);
            return 1;
        });
        StoragePathCoordinator storagePathCoordinator = new StoragePathCoordinator("/test");
        StorageAccessService storageAccessService = mock(StorageAccessService.class);
        when(storageAccessService.head(anyString())).thenReturn(new StorageObjectInfo(false, null, null, null));
        UserService userService = mock(UserService.class);
        when(userService.currentUserDetail()).thenReturn(User.builder().id(1L).build());
        ProjectService projectService = mock(ProjectService.class);
        when(projectService.findProject(anyString())).thenReturn(
                Project.builder().id(1L).build());
        when(projectService.findProject(anyString())).thenReturn(Project.builder().id(1L).build());

        HotJobHolder hotJobHolder = new HotJobHolderImpl(mock(MeterRegistry.class));

        DatasetDao datasetDao = mock(DatasetDao.class);

        IdConverter idConvertor = new IdConverter();
        VersionAliasConverter versionAliasConvertor = new VersionAliasConverter();

        DatasetUploader datasetUploader = new DatasetUploader(hotDatasetHolder, datasetMapper, datasetVersionMapper,
                storagePathCoordinator, storageAccessService, userService,
                hotJobHolder, projectService, dataStoreTableNameHelper, datasetDao, idConvertor,
                versionAliasConvertor);

        DatasetUploadRequest uploadRequest = new DatasetUploadRequest();
        String dsName = "testds3";
        Long dsVersionId = 1L;
        uploadRequest.setUploadId(dsVersionId);
        uploadRequest.setSwds(dsName + ":" + dsVersionId);
        datasetUploader.create(HotDatasetHolderTest.MANIFEST, "_manifest.yaml", uploadRequest);
        datasetUploader.uploadHashedBlob(
                dsVersionId,
                new MockMultipartFile("index.jsonl", "index.jsonl", "plain/text", index_file_content.getBytes()),
                "abc/index.jsonl");

        when(datasetDao.selectVersionOrderForUpdate(any(), any())).thenReturn(1L);
        when(datasetDao.selectMaxVersionOrderOfBundleForUpdate(any())).thenReturn(2L);
        when(datasetDao.updateVersionOrder(any(), any())).thenReturn(1);

        datasetUploader.end(dsVersionId);

        verify(storageAccessService).put(anyString(), any(InputStream.class));
        verify(datasetVersionMapper).updateStatus(1L, DatasetVersion.STATUS_AVAILABLE);
        verify(datasetVersionMapper).insert(any(DatasetVersionEntity.class));
        verify(datasetMapper).findByName(eq(dsName), anyLong(), any());
        verify(datasetMapper).insert(any(DatasetEntity.class));

        when(storageAccessService.list(anyString())).thenReturn(Stream.of("a", "b"));
        datasetUploader.create(HotDatasetHolderTest.MANIFEST, "_manifest.yaml", uploadRequest);
        datasetUploader.cancel(dsVersionId);
        verify(storageAccessService, times(0)).delete("c");

        when(datasetMapper.findByName(eq(dsName), anyLong(), any())).thenReturn(
                DatasetEntity.builder().id(1L).projectId(1L).build());
        DatasetVersionEntity mockedEntity = DatasetVersionEntity.builder()
                .id(1L)
                .versionName("testversion")
                .status(DatasetVersion.STATUS_AVAILABLE)
                .versionMeta(HotDatasetHolderTest.MANIFEST)
                .build();
        String dsVersion = "mizwkzrqgqzdemjwmrtdmmjummzxczi3";
        when(datasetVersionMapper.findByNameAndDatasetId(dsVersion, 1L, true)).thenReturn(mockedEntity);
        when(datasetVersionMapper.findByNameAndDatasetId(dsVersion, 1L, true)).thenReturn(mockedEntity);
        when(datasetVersionMapper.find(1L)).thenReturn(mockedEntity);
        when(storageAccessService.get(anyString())).thenReturn(
                new LengthAbleInputStream(
                        new ByteArrayInputStream(index_file_content.getBytes()),
                        index_file_content.getBytes().length
                )
        );
        when(datasetDao.findByNameForUpdate(anyString(), anyLong()))
                .thenReturn(DatasetEntity.builder().id(1L).build());
        when(datasetDao.findVersionByNameAndBundleId(dsVersion, 1L))
                .thenReturn(mockedEntity);
        HttpServletResponse httpResponse = mock(HttpServletResponse.class);
        ServletOutputStream mockOutPutStream = new ServletOutputStream() {

            @Override
            public boolean isReady() {
                return false;
            }

            @Override
            public void setWriteListener(WriteListener writeListener) {

            }

            @Override
            public void write(int b) throws IOException {

            }

        };
        when(httpResponse.getOutputStream()).thenReturn(mockOutPutStream);
        datasetUploader.pull("project", dsName, dsVersion, "index.jsonl", httpResponse);

        Assertions.assertThrowsExactly(SwValidationException.class,
                () -> datasetUploader.create(HotDatasetHolderTest.MANIFEST, "_manifest.yaml", uploadRequest));

        JobMockHolder jobMockHolder = new JobMockHolder();
        Job mockJob = jobMockHolder.mockJob();
        hotJobHolder.adopt(mockJob);
        uploadRequest.setForce("1");
        Assertions.assertThrowsExactly(SwValidationException.class,
                () -> datasetUploader.create(HotDatasetHolderTest.MANIFEST, "_manifest.yaml", uploadRequest));
        hotJobHolder.remove(mockJob.getId());
        datasetUploader.create(HotDatasetHolderTest.MANIFEST, "_manifest.yaml", uploadRequest);
        verify(datasetVersionMapper, times(1)).updateStatus(1L, DatasetVersion.STATUS_UN_AVAILABLE);

    }

    static final String index_file_content = "/*\n"
            + " * Copyright 2022 Starwhale, Inc. All Rights Reserved.\n"
            + " *\n"
            + " * Licensed under the Apache License, Version 2.0 (the \"License\");\n"
            + " * you may not use this file except in compliance with the License.\n"
            + " * You may obtain a copy of the License at\n"
            + " *\n"
            + " * http://www.apache.org/licenses/LICENSE-2.0\n"
            + " *\n"
            + " * Unless required by applicable law or agreed to in writing, software\n"
            + " * distributed under the License is distributed on an \"AS IS\" BASIS,\n"
            + " * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.\n"
            + " * See the License for the specific language governing permissions and\n"
            + " * limitations under the License.\n"
            + " */";
}
