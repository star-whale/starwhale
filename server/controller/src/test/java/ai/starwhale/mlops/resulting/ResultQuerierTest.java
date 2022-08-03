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

package ai.starwhale.mlops.resulting;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import ai.starwhale.mlops.domain.job.mapper.JobMapper;
import ai.starwhale.mlops.domain.job.po.JobEntity;
import ai.starwhale.mlops.domain.job.status.JobStatus;
import ai.starwhale.mlops.exception.SWProcessException;
import ai.starwhale.mlops.exception.SWValidationException;
import ai.starwhale.mlops.storage.StorageAccessService;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Map;
import java.util.stream.Stream;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;


/**
 * test for {@link  ai.starwhale.mlops.resulting.ResultQuerier}
 */
public class ResultQuerierTest {

    final String resultOutputPath = "OUTPUT_PATH/PATH";

    final String resultOutputPathResult = "OUTPUT_PATH/PATH/result";

    final String resultFileOutputPath = "OUTPUT_PATH/PATH/RESULT";

    final String exceptionPath = "exceptionPath";
    final String exceptionPathResult = "exceptionPath/result";

    final String mockResultOutputPath = "mock_result.json";

    final String mockResultOutputPathResult = "mock_result.json/result";

    final String mockResultOutputPathResult1 = "mock_result.json/result1";

    ObjectMapper objectMapper = new ObjectMapper();

    final String OBJECT="{\"name\":\"zhangsan\"}";

    final String MOCK_RESULT  = "{\"kind\":\"multi_classification\",\"summary\":{\"accuracy\":0.9894,\"macro avg\":{\"precision\":0.9893024234074879,\"recall\":0.9893615007448199,\"f1-score\":0.9893068278560861,\"support\":10000},\"weighted avg\":{\"precision\":0.9894464486215673,\"recall\":0.9894,\"f1-score\":0.9893988328111353,\"support\":10000},\"hamming_loss\":0.0106,\"cohen_kappa_score\":0.9882177460130186},\"labels\":{\"0\":{\"precision\":0.9838872104733132,\"recall\":0.996938775510204,\"f1-score\":0.9903699949315762,\"support\":980},\"1\":{\"precision\":0.9938488576449912,\"recall\":0.9964757709251101,\"f1-score\":0.9951605807303123,\"support\":1135},\"2\":{\"precision\":0.9818007662835249,\"recall\":0.9932170542635659,\"f1-score\":0.98747591522158,\"support\":1032},\"3\":{\"precision\":0.997997997997998,\"recall\":0.9871287128712871,\"f1-score\":0.9925335988053758,\"support\":1010},\"4\":{\"precision\":0.9938837920489296,\"recall\":0.9928716904276986,\"f1-score\":0.9933774834437087,\"support\":982},\"5\":{\"precision\":0.9790518191841234,\"recall\":0.9955156950672646,\"f1-score\":0.9872151195108393,\"support\":892},\"6\":{\"precision\":0.9926238145416227,\"recall\":0.9832985386221295,\"f1-score\":0.9879391714735186,\"support\":958},\"7\":{\"precision\":0.9882926829268293,\"recall\":0.9854085603112841,\"f1-score\":0.9868485143692158,\"support\":1028},\"8\":{\"precision\":0.9896694214876033,\"recall\":0.9835728952772074,\"f1-score\":0.9866117404737382,\"support\":974},\"9\":{\"precision\":0.9919678714859438,\"recall\":0.979187314172448,\"f1-score\":0.9855361596009976,\"support\":1009}}}";

    @Test
    public void testResultOfJob() throws IOException {
        JobMapper jobMapper = mockJobMapper();
        StorageAccessService storageAccessService = mockStorageAccessService();
        ResultQuerier resultQuerier = new ResultQuerier(jobMapper,storageAccessService,new ObjectMapper());
        Object o = resultQuerier.resultOfJob(1L);
        Assertions.assertEquals(OBJECT,objectMapper.writeValueAsString(o));
        Map<String, Object> result = resultQuerier.flattenResultOfJob(6L);
        Assertions.assertEquals(52, result.size());
        Assertions.assertEquals("multi_classification", result.get("kind"));
        Assertions.assertNotNull(result.get("labels/9/support"));
        Assertions.assertNotNull(result.get("summary/accuracy"));
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
        when(storageAccessService.list(mockResultOutputPathResult)).thenReturn(Stream.of(mockResultOutputPathResult1));
        when(storageAccessService.get(mockResultOutputPathResult1)).thenReturn(mockResultInputStream());
        return storageAccessService;
    }

    private InputStream mockInputStream() {
        return new ByteArrayInputStream(OBJECT.getBytes());
    }

    private InputStream mockResultInputStream() {
        return new ByteArrayInputStream(MOCK_RESULT.getBytes());
    }

    JobMapper mockJobMapper(){
        JobMapper jobMapper = mock(JobMapper.class);
        when(jobMapper.findJobById(1L)).thenReturn(mockJobEntity(1l,JobStatus.SUCCESS,resultOutputPath));
        when(jobMapper.findJobById(3L)).thenReturn(mockJobEntity(3l,JobStatus.RUNNING,resultOutputPath));
        when(jobMapper.findJobById(4L)).thenReturn(mockJobEntity(4l,JobStatus.SUCCESS,"resultOutputPath"));
        when(jobMapper.findJobById(5L)).thenReturn(mockJobEntity(5l,JobStatus.SUCCESS,
            exceptionPath));
        when(jobMapper.findJobById(6L)).thenReturn(mockJobEntity(6l,JobStatus.SUCCESS,mockResultOutputPath));
        return jobMapper;
    }

    private JobEntity mockJobEntity(Long jobId,JobStatus jobStatus,String resultPath) {
        return JobEntity.builder().id(jobId).jobStatus(jobStatus).resultOutputPath(
            resultPath).build();
    }
}
