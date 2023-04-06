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

package ai.starwhale.mlops.domain.job.converter;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.hasProperty;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.isA;
import static org.hamcrest.Matchers.notNullValue;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.mock;
import static org.mockito.Mockito.when;

import ai.starwhale.mlops.api.protocol.runtime.RuntimeVo;
import ai.starwhale.mlops.api.protocol.user.UserVo;
import ai.starwhale.mlops.common.IdConverter;
import ai.starwhale.mlops.domain.dataset.DatasetDao;
import ai.starwhale.mlops.domain.job.po.JobEntity;
import ai.starwhale.mlops.domain.job.status.JobStatus;
import ai.starwhale.mlops.domain.model.po.ModelVersionEntity;
import ai.starwhale.mlops.domain.runtime.RuntimeService;
import ai.starwhale.mlops.domain.system.SystemSettingService;
import ai.starwhale.mlops.domain.system.resourcepool.bo.ResourcePool;
import ai.starwhale.mlops.domain.user.po.UserEntity;
import ai.starwhale.mlops.schedule.k8s.K8sClient;
import java.util.Date;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class JobConverterTest {

    private JobConverter jobConvertor;

    @BeforeEach
    public void setUp() {
        RuntimeService runtimeService = mock(RuntimeService.class);
        given(runtimeService.findRuntimeByVersionIds(anyList()))
                .willReturn(List.of(RuntimeVo.builder().id("1").build()));
        DatasetDao datasetDao = mock(DatasetDao.class);
        IdConverter idConvertor = new IdConverter();
        SystemSettingService systemSettingService = mock(SystemSettingService.class);
        var k8sClient = mock(K8sClient.class);
        when(systemSettingService.queryResourcePool(anyString())).thenReturn(ResourcePool.defaults(k8sClient));
        jobConvertor = new JobConverter(
                idConvertor,
                runtimeService,
                datasetDao,
                systemSettingService
        );
    }

    @Test
    public void testConvert() {
        JobEntity entity = JobEntity.builder()
                .id(1L)
                .jobUuid("job-uuid")
                .owner(UserEntity.builder().build())
                .modelName("model")
                .modelVersion(ModelVersionEntity.builder().versionName("v1").build())
                .runtimeVersionId(1L)
                .createdTime(new Date(1000L))
                .jobStatus(JobStatus.SUCCESS)
                .finishedTime(new Date(1001L))
                .comment("job-comment")
                .resourcePool("rp")
                .build();

        var res = jobConvertor.convert(entity);
        assertThat(res, allOf(
                notNullValue(),
                hasProperty("id", is("1")),
                hasProperty("uuid", is("job-uuid")),
                hasProperty("owner", isA(UserVo.class)),
                hasProperty("modelName", is("model")),
                hasProperty("modelVersion", is("v1")),
                hasProperty("createdTime", is(1000L)),
                hasProperty("runtime", isA(RuntimeVo.class)),
                hasProperty("datasets", isA(List.class)),
                hasProperty("jobStatus", is(JobStatus.SUCCESS)),
                hasProperty("stopTime", is(1001L)),
                hasProperty("comment", is("job-comment")),
                hasProperty("resourcePool", is("default"))
        ));
    }
}
