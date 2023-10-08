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
import ai.starwhale.mlops.domain.bundle.tag.BundleVersionTagDao;
import ai.starwhale.mlops.domain.dataset.bo.DatasetVersion;
import ai.starwhale.mlops.domain.dataset.index.datastore.DataStoreTableNameHelper;
import ai.starwhale.mlops.domain.dataset.mapper.DatasetMapper;
import ai.starwhale.mlops.domain.dataset.mapper.DatasetVersionMapper;
import ai.starwhale.mlops.domain.dataset.po.DatasetEntity;
import ai.starwhale.mlops.domain.dataset.po.DatasetVersionEntity;
import ai.starwhale.mlops.domain.dataset.upload.DatasetUploader;
import ai.starwhale.mlops.domain.job.bo.Job;
import ai.starwhale.mlops.domain.job.cache.HotJobHolder;
import ai.starwhale.mlops.domain.job.cache.HotJobHolderImpl;
import ai.starwhale.mlops.domain.project.ProjectService;
import ai.starwhale.mlops.domain.project.bo.Project;
import ai.starwhale.mlops.domain.storage.StoragePathCoordinator;
import ai.starwhale.mlops.domain.user.UserService;
import ai.starwhale.mlops.domain.user.bo.User;
import ai.starwhale.mlops.exception.api.StarwhaleApiException;
import ai.starwhale.mlops.storage.LengthAbleInputStream;
import ai.starwhale.mlops.storage.StorageAccessService;
import ai.starwhale.mlops.storage.StorageObjectInfo;
import io.micrometer.core.instrument.MeterRegistry;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.stream.Stream;
import javax.servlet.ServletOutputStream;
import javax.servlet.WriteListener;
import javax.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.stubbing.Answer;
import org.springframework.web.multipart.MultipartFile;

/**
 * test for {@link DatasetUploader}
 */
public class DatasetUploaderTest {

    final DataStoreTableNameHelper dataStoreTableNameHelper = new DataStoreTableNameHelper();
    public static final String MANIFEST = "build:\n"
            + "  os: Linux\n"
            + "  sw_version: 0.1.0\n"
            + "created_at: 2022-04-02 09:17:45 CST\n"
            + "dataset_attr:\n"
            + "  alignment_size: 4096\n"
            + "  batch_size: 50\n"
            + "  volume_size: 8388608\n"
            + "dataset_byte_size: 63247649\n"
            + "dep:\n"
            + "  conda:\n"
            + "    use: true\n"
            + "  dep:\n"
            + "    local_gen_env: false\n"
            + "  env: conda\n"
            + "  local_gen_env: false\n"
            + "  python: 3.7.11\n"
            + "  system: Linux\n"
            + "  venv:\n"
            + "    use: false\n"
            + "extra:\n"
            + "  desc: MNIST data and label test dataset\n"
            + "  tag:\n"
            + "  - bin\n"
            + "mode: generate\n"
            + "process: mnist.process:DataSetProcessExecutor\n"
            + "dataset_byte_size: 7840016\n"
            + "dataset_summary:\n"
            + "  data_byte_size: 7840000\n"
            + "  include_link: false\n"
            + "  include_user_raw: true\n"
            + "  increased_rows: 10000\n"
            + "  label_byte_size: 10000\n"
            + "  rows: 10000\n"
            + "  unchanged_rows: 0\n"
            + "signature:\n"
            + "  - 8759448:blake2b:01229e38ff899bf52c19108ddc3e67512660bbaa92d9a84568f1aedec97b8dad87b48bf5629b"
            + "f575b473b5b73987ab6254feb91a3ede674936637e3e4f25a72d\n"
            + "  - 8759448:blake2b:83e84a527498c596d3687b234bc03e16cff44583118d4ef5a3c2d3f9ae3f559466a778899017"
            + "e0839bd6b6dcc8fa116e805fe4765abe71028f0f4838db1f253c\n"
            + "  - 8759448:blake2b:9182b715d6e40de19760b954d0fe8fe1876800c637f4025c202d99ce16682a979348149b6823"
            + "8e05a483279f626076ec15800c61c18eba5dff372f092d83450e\n"
            + "  - 8759448:blake2b:0d942dca26aa4fd223e94cdbc052d663913de17a62284517369e1ea07339b363e31420c8e125"
            + "a29fef16284e8554523b778cefe024eb8f2707bfe3ecde6988e8\n"
            + "  - 8759448:blake2b:5e34b2682ec109d30ec75535a49ef07b316e01ff0b1c9d24b44462a14e1eb8a70dd860e93da0"
            + "a182b6e889d0478cea833a2fce80f2f044d33f72b2186aef2077\n"
            + "  - 8759448:blake2b:66b1063cff080b979961d4c0888e15815cd90d71b9e55f55ea6a08fe53b5a34af80d312e2b9d"
            + "2317cc22016c865425247e02cc07e2c25fff4309bcfb5637c0de\n"
            + "  - 4748112:blake2b:c741a24b8a12453dc53c2e4f092617494b7cb5838dbc9530adc9a36604e0b535669f31bc51ba"
            + "96540f32fa6f88f247ddf9a8d386e3e2cf020a9e463b9d1c4cd7\n"
            + "  - 247649:blake2b:0b8ced80a1e4eeb92b343778504cb749d934a0dfe86f5cd53f7c456c6c9b9cab1b1e83f570556"
            + "eb087da34f6d64d996a55ebf52e7707dc23d2832b9b8e7067b3\n"
            + "  - 870552:blake2b:1c36fe21b21d4fc9e3de7860610f1a18750b1b944f5d6a2998686e24197e38081fbed29bfdb3a"
            + "103e3fe7169c037387403fd2586662c58dad86c3566e528a729\n"
            + "  - 870552:blake2b:2b288ea08d37c0b1dd5b5ace425cc55841b9e37dad520515c7124151b213db0e2f43b7491893e"
            + "a4936ee3fb1133d14f07063ec2a71b5fc88f967eb551a5bb925\n"
            + "  - 870552:blake2b:032d59d599f40661b887690d4d0041e10c54da7732d25956425a9dfd8f5311175592a4ef1d33a"
            + "f1f3e96ae8890c854041b29b31f2da08461f5ffad31149f2086\n"
            + "  - 870552:blake2b:3c2c7dcafc6059572dd5563fa671204d8058c779935566d95fb7b650c96fce9cb923cc6bbb088"
            + "be69a2a57ce89710f5716656f3575d76f04d0730b594c8058f2\n"
            + "  - 870552:blake2b:8aa901f51d9a610ae2aedcd3b74c9841fff3491746503c218e0ba44b8da4a2ecd433b53979126"
            + "38a62c70923316c764737b25aa1c168d917127fd4209e3e856f\n"
            + "  - 870552:blake2b:d7585e1b493311361d57335fc900697250d909ed605b3254bfcf8495ee585a3f0889a05c73501"
            + "5a26a33248d5dec608dc0602e8b3d86b0508293eda16922b8d3\n"
            + "  - 471888:blake2b:e1966727c307b611efacf19194e0a6a1099f09cab65b51ff81425967cf6d10cf77dfbc4855ac7"
            + "f117bfd4c7e753124cb2b83c6672b9820b42601a73e009e561f\n"
            + "version: mizwkzrqgqzdemjwmrtdmmjummzxczi3\n";

    @Test
    public void testDatasetUploader() throws IOException {
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

        DatasetUploader datasetUploader = new DatasetUploader(
                datasetMapper,
                datasetVersionMapper,
                mock(BundleVersionTagDao.class),
                storagePathCoordinator,
                storageAccessService,
                userService,
                hotJobHolder,
                projectService,
                dataStoreTableNameHelper,
                datasetDao,
                idConvertor,
                versionAliasConvertor
        );

        DatasetUploadRequest uploadRequest = new DatasetUploadRequest();
        String dsName = "testds3";
        Long dsVersionId = 1L;
        uploadRequest.setUploadId(dsVersionId);
        uploadRequest.setSwds(dsName + ":" + dsVersionId);
        uploadRequest.setProject("project");
        datasetUploader.create(MANIFEST, "_manifest.yaml", uploadRequest);

        when(datasetDao.selectVersionOrderForUpdate(any(), any())).thenReturn(1L);
        when(datasetDao.selectMaxVersionOrderOfBundleForUpdate(any())).thenReturn(2L);
        when(datasetDao.updateVersionOrder(any(), any())).thenReturn(1);

        datasetUploader.end(dsVersionId);

        verify(datasetVersionMapper).updateStatus(1L, DatasetVersion.STATUS_AVAILABLE);
        verify(datasetVersionMapper).insert(any(DatasetVersionEntity.class));
        verify(datasetMapper).findByName(eq(dsName), anyLong(), any());
        verify(datasetMapper).insert(any(DatasetEntity.class));

        when(storageAccessService.list(anyString())).thenReturn(Stream.of("a", "b"));
        datasetUploader.create(MANIFEST, "_manifest.yaml", uploadRequest);
        datasetUploader.cancel(dsVersionId);
        verify(storageAccessService, times(0)).delete("c");

        when(datasetMapper.findByName(eq(dsName), anyLong(), any())).thenReturn(
                DatasetEntity.builder().id(1L).projectId(1L).build());
        DatasetVersionEntity mockedEntity = DatasetVersionEntity.builder()
                .id(1L)
                .versionName("testversion")
                .status(DatasetVersion.STATUS_AVAILABLE)
                .versionMeta(MANIFEST)
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

        Assertions.assertThrowsExactly(StarwhaleApiException.class,
                () -> datasetUploader.create(MANIFEST, "_manifest.yaml", uploadRequest));

        JobMockHolder jobMockHolder = new JobMockHolder();
        Job mockJob = jobMockHolder.mockJob();
        hotJobHolder.adopt(mockJob);
        uploadRequest.setForce("1");
        Assertions.assertThrowsExactly(StarwhaleApiException.class,
                () -> datasetUploader.create(MANIFEST, "_manifest.yaml", uploadRequest));
        hotJobHolder.remove(mockJob.getId());
        datasetUploader.create(MANIFEST, "_manifest.yaml", uploadRequest);
        verify(datasetVersionMapper, times(1)).updateStatus(1L, DatasetVersion.STATUS_UN_AVAILABLE);

    }

    @Test
    public void testUploadHashedBlob() throws IOException {
        StoragePathCoordinator storagePathCoordinator = new StoragePathCoordinator("/test");
        StorageAccessService storageAccessService = mock(StorageAccessService.class);
        when(storageAccessService.head(anyString())).thenReturn(new StorageObjectInfo(false, null, null, null));
        UserService userService = mock(UserService.class);
        when(userService.currentUserDetail()).thenReturn(User.builder().id(1L).build());
        ProjectService projectService = mock(ProjectService.class);
        when(projectService.findProject(anyString())).thenReturn(
                Project.builder().id(1L).build());
        when(projectService.findProject(anyString())).thenReturn(Project.builder().id(1L).build());
        DatasetUploader datasetUploader = new DatasetUploader(
                mock(DatasetMapper.class),
                mock(DatasetVersionMapper.class),
                mock(BundleVersionTagDao.class),
                storagePathCoordinator,
                storageAccessService,
                userService,
                mock(HotJobHolder.class),
                projectService,
                dataStoreTableNameHelper,
                mock(DatasetDao.class),
                mock(IdConverter.class),
                mock(VersionAliasConverter.class)
        );
        MultipartFile file = mock(MultipartFile.class);
        when(file.getInputStream()).thenReturn(new ByteArrayInputStream("fc".getBytes(StandardCharsets.UTF_8)));
        datasetUploader.uploadHashedBlob("p", "ds", file, "aabbc");
        ArgumentCaptor<InputStream> argumentCaptor = ArgumentCaptor.forClass(InputStream.class);
        verify(storageAccessService).put(
                eq("/test/controller/project/1/common-dataset/ds/aa/aabbc"),
                argumentCaptor.capture()
        );
        Assertions.assertEquals("fc", new String(argumentCaptor.getValue().readAllBytes()));
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
