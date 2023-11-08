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

package ai.starwhale.mlops.domain.sft;

import ai.starwhale.mlops.api.protocol.sft.SftCreateRequest;
import ai.starwhale.mlops.domain.job.JobCreator;
import ai.starwhale.mlops.domain.job.JobType;
import ai.starwhale.mlops.domain.job.bo.Job;
import ai.starwhale.mlops.domain.job.bo.UserJobCreateRequest;
import ai.starwhale.mlops.domain.job.mapper.JobMapper;
import ai.starwhale.mlops.domain.job.po.JobEntity;
import ai.starwhale.mlops.domain.project.bo.Project;
import ai.starwhale.mlops.domain.sft.po.SftEntity;
import ai.starwhale.mlops.domain.sft.po.SftMapper;
import ai.starwhale.mlops.domain.sft.vo.SftVo;
import ai.starwhale.mlops.domain.user.bo.User;
import com.github.pagehelper.PageHelper;
import com.github.pagehelper.PageInfo;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class SftService {

    JobCreator jobCreator;

    SftMapper sftMapper;

    JobMapper jobMapper;


    public void createSft(
            Long spaceId,
            Project project,
            SftCreateRequest request,
            User creator
    ) {
        Job job = jobCreator.createJob(
                UserJobCreateRequest.builder()
                        .modelVersionId(request.getModelVersionId())
                        .runtimeVersionId(request.getRuntimeVersionId())
                        .datasetVersionIds(request.getDatasetVersionIds())
                        .devMode(request.isDevMode())
                        .devPassword(request.getDevPassword())
                        .ttlInSec(request.getTimeToLiveInSec())
                        .project(project)
                        .user(creator)
                        .comment(request.getComment())
                        .resourcePool(request.getResourcePool())
                        .handler(request.getHandler())
                        .stepSpecOverWrites(request.getStepSpecOverWrites())
                        .jobType(JobType.FINE_TUNE)
                        .build()
        );
        sftMapper.add(
                SftEntity.builder()
                        .jobId(job.getId())
                        .spaceId(spaceId)
                        .evalDatasets(request.getEvalDatasetVersionIds())
                        .trainDatasets(request.getDatasetVersionIds())
                        .baseModelVersionId(request.getModelVersionId())
                        .build()
        );
    }


    public PageInfo<SftVo> listSft(Long spaceId, Integer pageNum, Integer pageSize) {
        try (var ph = PageHelper.startPage(pageNum, pageSize)) {
            return PageInfo.of(sftMapper.list(spaceId).stream().map(sftEntity -> {
                Long jobId = sftEntity.getJobId();
                JobEntity job = jobMapper.findJobById(jobId);
                sftEntity.getEvalDatasets();
                sftEntity.getTrainDatasets();
                sftEntity.getBaseModelVersionId();
                sftEntity.getTargetModelVersionId();
                return SftVo.builder()
                        .id(sftEntity.getId())
                        .jobId(jobId)
                        .status(job.getJobStatus())
                        .startTime(job.getCreatedTime().getTime())
                        .endTime(job.getFinishedTime().getTime())
                        .evalDatasets(List.of())//TODO
                        .trainDatasets(List.of())//TODO
                        .baseModel(null)//TODO
                        .targetModel(null)//TODO
                        .build();
            }).collect(Collectors.toList()));
        }
    }

    public void evalSft(List<Long> evalDatasetIds, Long runtimeId, String handerSpec, Map<String, String> envs) {

    }

    public void releaseSft(Long sftId) {

    }
}
