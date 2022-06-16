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

package ai.starwhale.test.domain.swds;


import static ai.starwhale.mlops.domain.swds.po.SWDatasetVersionEntity.STATUS_AVAILABLE;
import static ai.starwhale.mlops.domain.swds.po.SWDatasetVersionEntity.STATUS_UN_AVAILABLE;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyIterable;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import ai.starwhale.mlops.api.protocol.swds.upload.UploadRequest;
import ai.starwhale.mlops.configuration.json.ObjectMapperConfig;
import ai.starwhale.mlops.domain.job.bo.Job;
import ai.starwhale.mlops.domain.job.cache.HotJobHolder;
import ai.starwhale.mlops.domain.job.cache.HotJobHolderImpl;
import ai.starwhale.mlops.domain.job.status.JobStatus;
import ai.starwhale.mlops.domain.project.ProjectManager;
import ai.starwhale.mlops.domain.project.mapper.ProjectMapper;
import ai.starwhale.mlops.domain.storage.StoragePathCoordinator;
import ai.starwhale.mlops.domain.swds.mapper.SWDatasetMapper;
import ai.starwhale.mlops.domain.swds.mapper.SWDatasetVersionMapper;
import ai.starwhale.mlops.domain.swds.po.SWDatasetEntity;
import ai.starwhale.mlops.domain.swds.po.SWDatasetVersionEntity;
import ai.starwhale.mlops.domain.swds.upload.HotSwdsHolder;
import ai.starwhale.mlops.domain.swds.upload.SWDSVersionWithMetaConverter;
import ai.starwhale.mlops.domain.swds.upload.SwdsUploader;
import ai.starwhale.mlops.domain.user.UserService;
import ai.starwhale.mlops.domain.user.bo.User;
import ai.starwhale.mlops.exception.SWValidationException;
import ai.starwhale.mlops.storage.StorageAccessService;
import ai.starwhale.test.JobMockHolder;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.List;
import java.util.Set;
import javax.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockMultipartFile;

/**
 * test for {@link SwdsUploader}
 */
public class SwdsUploaderTest {

    @Test
    public void testSwdsUploader() throws IOException {
        ObjectMapper yamlMapper = new ObjectMapperConfig().yamlMapper();
        SWDSVersionWithMetaConverter swdsVersionWithMetaConverter = new SWDSVersionWithMetaConverter(
            yamlMapper);
        HotSwdsHolder hotSwdsHolder = new HotSwdsHolder(swdsVersionWithMetaConverter);
        SWDatasetMapper swdsMapper = mock(SWDatasetMapper.class);
        SWDatasetVersionMapper swdsVersionMapper = mock(SWDatasetVersionMapper.class);
        StoragePathCoordinator storagePathCoordinator = new StoragePathCoordinator("/test");
        StorageAccessService storageAccessService = mock(StorageAccessService.class);
        UserService userService = mock(UserService.class);
        when(userService.currentUserDetail()).thenReturn(User.builder().idTableKey(1L).build());
        ProjectMapper projectMapper = mock(ProjectMapper.class);
        ProjectManager projectManager = mock(ProjectManager.class);

        HotJobHolder hotJobHolder = new HotJobHolderImpl();

        SwdsUploader swdsUploader = new SwdsUploader(hotSwdsHolder, swdsMapper, swdsVersionMapper,
            storagePathCoordinator, storageAccessService, userService, projectMapper, yamlMapper,
            hotJobHolder, projectManager);

        swdsUploader.create(HotSwdsHolderTest.MANIFEST,"_manifest.yaml", new UploadRequest());
        String dsVersionId = "mizwkzrqgqzdemjwmrtdmmjummzxczi3";
        swdsUploader.uploadBody(
            dsVersionId,new MockMultipartFile("index.jsonl","index.jsonl","plain/text",index_file_content.getBytes()));
        swdsUploader.uploadBody(
            dsVersionId,new MockMultipartFile("index.jsonl","index.jsonl","plain/text",index_file_content.getBytes()));
        swdsUploader.uploadBody(
            dsVersionId,new MockMultipartFile("index.jsonl","index.jsonl","plain/text",index_file_content.getBytes()));
        swdsUploader.uploadBody(
            dsVersionId,new MockMultipartFile("index.jsonl","index.jsonl","plain/text",index_file_content.getBytes()));



        Assertions.assertThrowsExactly(SWValidationException.class,()->{
            swdsUploader.uploadBody(
                dsVersionId,new MockMultipartFile("index.jsonl","index.jsonl","plain/text","index_file_content".getBytes()));
        });
        swdsUploader.uploadBody(
            dsVersionId,new MockMultipartFile("index.jsonl-2","index.jsonl-2","plain/text","index_file_content".getBytes()));

        swdsUploader.end(dsVersionId);

        verify(storageAccessService,times(3)).put(anyString(),any(byte[].class));
        verify(swdsVersionMapper).updateStatus(null, STATUS_AVAILABLE);
        verify(swdsVersionMapper).addNewVersion(any(SWDatasetVersionEntity.class));
        String dsName = "testds3";
        verify(swdsMapper).findByName(eq(dsName),anyLong());
        verify(swdsMapper).addDataset(any(SWDatasetEntity.class));


        when(storageAccessService.list(anyString())).thenReturn(List.of("a","b").stream());
        swdsUploader.create(HotSwdsHolderTest.MANIFEST,"_manifest.yaml", new UploadRequest());
        swdsUploader.cancel(dsVersionId);
        verify(swdsVersionMapper).deleteById(null);
        verify(storageAccessService).list(anyString());
        verify(storageAccessService).delete("a");
        verify(storageAccessService).delete("b");
        verify(storageAccessService,times(0)).delete("c");


        when(swdsMapper.findByName(eq(dsName),anyLong())).thenReturn(SWDatasetEntity.builder().id(1L).build());
        SWDatasetVersionEntity mockedEntity = SWDatasetVersionEntity.builder()
            .id(1L)
            .versionName("testversion")
            .status(STATUS_AVAILABLE)
            .build();
        when(swdsVersionMapper.findByDSIdAndVersionNameForUpdate(1L,dsVersionId)).thenReturn(mockedEntity);
        when(swdsVersionMapper.findByDSIdAndVersionName(1L,dsVersionId)).thenReturn(mockedEntity);
        when(storageAccessService.get(anyString())).thenReturn(new ByteArrayInputStream(index_file_content.getBytes()));
        byte[] indexbytes = swdsUploader.pull("project",dsName, dsVersionId, "index.jsonl",
            mock(HttpServletResponse.class));

        Assertions.assertEquals(index_file_content,new String(indexbytes));;

        Assertions.assertThrowsExactly(SWValidationException.class,()->{
            swdsUploader.create(HotSwdsHolderTest.MANIFEST,"_manifest.yaml", new UploadRequest());
        });

        JobMockHolder jobMockHolder = new JobMockHolder();
        Job mockJob = jobMockHolder.mockJob();
        hotJobHolder.adopt(mockJob);
        UploadRequest uploadRequest = new UploadRequest();
        uploadRequest.setForce("1");
        Assertions.assertThrowsExactly(SWValidationException.class,()->{
            swdsUploader.create(HotSwdsHolderTest.MANIFEST,"_manifest.yaml", uploadRequest);
        });
        hotJobHolder.remove(mockJob.getId());
        swdsUploader.create(HotSwdsHolderTest.MANIFEST,"_manifest.yaml", uploadRequest);
        verify(swdsVersionMapper,times(1)).updateStatus(1l, STATUS_UN_AVAILABLE);



    }

    static final String index_file_content="/*\n"
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

