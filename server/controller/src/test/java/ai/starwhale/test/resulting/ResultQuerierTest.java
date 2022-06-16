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

package ai.starwhale.test.resulting;

import ai.starwhale.mlops.domain.job.mapper.JobMapper;
import ai.starwhale.mlops.domain.job.po.JobEntity;
import ai.starwhale.mlops.domain.job.status.JobStatus;
import ai.starwhale.mlops.exception.SWProcessException;
import ai.starwhale.mlops.exception.SWValidationException;
import ai.starwhale.mlops.resulting.ResultQuerier;
import ai.starwhale.mlops.storage.StorageAccessService;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.stream.Stream;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import static org.mockito.Mockito.*;


/**
 * test for {@link  ai.starwhale.mlops.resulting.ResultQuerier}
 */
public class ResultQuerierTest {

    final String resultOutputPath = "OUTPUT_PATH/PATH";

    final String resultOutputPathResult = "OUTPUT_PATH/PATH/result";

    final String resultFileOutputPath = "OUTPUT_PATH/PATH/RESULT";

    final String exceptionPath = "exceptionPath";
    final String exceptionPathResult = "exceptionPath/result";

    ObjectMapper objectMapper = new ObjectMapper();

    final String OBJECT="{\"name\":\"zhangsan\"}";

    @Test
    public void testResultOfJob() throws IOException {
        JobMapper jobMapper = mockJobMapper();
        StorageAccessService storageAccessService = mockStorageAccessService();
        ResultQuerier resultQuerier = new ResultQuerier(jobMapper,storageAccessService,new ObjectMapper());
        Object o = resultQuerier.resultOfJob(1L);
        Assertions.assertEquals(OBJECT,objectMapper.writeValueAsString(o));
    }

    @Test
    public void testResultOfJobException() throws IOException {
        JobMapper jobMapper = mockJobMapper();
        StorageAccessService storageAccessService = mockStorageAccessService();
        ResultQuerier resultQuerier = new ResultQuerier(jobMapper,storageAccessService,new ObjectMapper());
        Assertions.assertThrowsExactly(SWValidationException.class,()->resultQuerier.resultOfJob(2L));
        Assertions.assertThrowsExactly(SWValidationException.class,()->resultQuerier.resultOfJob(3L));
        Assertions.assertThrowsExactly(SWValidationException.class,()->resultQuerier.resultOfJob(4L));
        Assertions.assertThrowsExactly(SWProcessException.class,()->resultQuerier.resultOfJob(5L));
    }

    private StorageAccessService mockStorageAccessService() throws IOException {
        StorageAccessService storageAccessService = mock(StorageAccessService.class);
        when(storageAccessService.list(resultOutputPathResult)).thenReturn(Stream.of(resultFileOutputPath));
        when(storageAccessService.get(resultFileOutputPath)).thenReturn(mockInputStream());
        when(storageAccessService.list(exceptionPathResult)).thenThrow(IOException.class);
        return storageAccessService;
    }

    private InputStream mockInputStream() {
        return new ByteArrayInputStream(OBJECT.getBytes());
    }

    JobMapper mockJobMapper(){
        JobMapper jobMapper = mock(JobMapper.class);
        when(jobMapper.findJobById(1L)).thenReturn(mockJobEntity(1l,JobStatus.SUCCESS,resultOutputPath));
        when(jobMapper.findJobById(3L)).thenReturn(mockJobEntity(3l,JobStatus.RUNNING,resultOutputPath));
        when(jobMapper.findJobById(4L)).thenReturn(mockJobEntity(4l,JobStatus.SUCCESS,"resultOutputPath"));
        when(jobMapper.findJobById(5L)).thenReturn(mockJobEntity(5l,JobStatus.SUCCESS,
            exceptionPath));
        return jobMapper;
    }

    private JobEntity mockJobEntity(Long jobId,JobStatus jobStatus,String resultPath) {
        return JobEntity.builder().id(jobId).jobStatus(jobStatus).resultOutputPath(
            resultPath).build();
    }
}
