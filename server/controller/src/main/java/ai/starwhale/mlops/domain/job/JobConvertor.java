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

package ai.starwhale.mlops.domain.job;

import ai.starwhale.mlops.api.protocol.job.JobVO;
import ai.starwhale.mlops.common.Convertor;
import ai.starwhale.mlops.common.IDConvertor;
import ai.starwhale.mlops.common.LocalDateTimeConvertor;
import ai.starwhale.mlops.domain.node.Device;
import ai.starwhale.mlops.domain.node.Device.Clazz;
import ai.starwhale.mlops.domain.user.UserConvertor;
import ai.starwhale.mlops.exception.ConvertException;
import java.util.Objects;
import javax.annotation.Resource;
import org.springframework.stereotype.Component;

@Component
public class JobConvertor implements Convertor<JobEntity, JobVO> {

    @Resource
    private IDConvertor idConvertor;

    @Resource
    private UserConvertor userConvertor;

    @Resource
    private BaseImageConvertor baseImageConvertor;

    @Resource
    private LocalDateTimeConvertor localDateTimeConvertor;

    @Override
    public JobVO convert(JobEntity jobEntity) throws ConvertException {
        return JobVO.builder()
            .id(idConvertor.convert(jobEntity.getId()))
            .uuid(jobEntity.getJobUuid())
            .owner(userConvertor.convert(jobEntity.getOwner()))
            .modelName(jobEntity.getModelName())
            .modelVersion(jobEntity.getSwmpVersion().getVersionName())
            .createdTime(localDateTimeConvertor.convert(jobEntity.getCreatedTime()))
            .baseImage(baseImageConvertor.convert(jobEntity.getBaseImage()))
            .device(getDeviceName(jobEntity.getDeviceType()))
            .deviceAmount(jobEntity.getDeviceAmount())
            .jobStatus(jobEntity.getJobStatus())
            .stopTime(localDateTimeConvertor.convert(jobEntity.getFinishedTime()))
            .build();
    }

    @Override
    public JobEntity revert(JobVO jobVO) throws ConvertException {
        Objects.requireNonNull(jobVO, "jobVO");
        return null;
    }

    private String getDeviceName(int deviceType) {
        if(deviceType > 0 && deviceType <= Clazz.values().length) {
            return Device.Clazz.values()[deviceType - 1].name();
        }
        return "UNDEFINED";
    }
}
