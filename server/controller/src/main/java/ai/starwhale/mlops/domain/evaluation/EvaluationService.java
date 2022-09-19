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

package ai.starwhale.mlops.domain.evaluation;

import ai.starwhale.mlops.api.protocol.evaluation.AttributeVo;
import ai.starwhale.mlops.api.protocol.evaluation.ConfigRequest;
import ai.starwhale.mlops.api.protocol.evaluation.ConfigVo;
import ai.starwhale.mlops.api.protocol.evaluation.SummaryVo;
import ai.starwhale.mlops.api.protocol.job.JobVo;
import ai.starwhale.mlops.common.IdConvertor;
import ai.starwhale.mlops.common.PageParams;
import ai.starwhale.mlops.common.util.PageUtil;
import ai.starwhale.mlops.domain.evaluation.bo.ConfigQuery;
import ai.starwhale.mlops.domain.evaluation.bo.SummaryFilter;
import ai.starwhale.mlops.domain.evaluation.mapper.ViewConfigMapper;
import ai.starwhale.mlops.domain.evaluation.po.ViewConfigEntity;
import ai.starwhale.mlops.domain.job.converter.JobConvertor;
import ai.starwhale.mlops.domain.job.mapper.JobMapper;
import ai.starwhale.mlops.domain.job.po.JobEntity;
import ai.starwhale.mlops.domain.job.status.JobStatus;
import ai.starwhale.mlops.domain.job.status.JobStatusMachine;
import ai.starwhale.mlops.domain.project.ProjectManager;
import ai.starwhale.mlops.domain.user.UserService;
import cn.hutool.core.io.FileUtil;
import cn.hutool.core.util.NumberUtil;
import cn.hutool.core.util.StrUtil;
import com.github.pagehelper.PageHelper;
import com.github.pagehelper.PageInfo;
import com.google.common.collect.Lists;
import java.nio.charset.Charset;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;

@Service
public class EvaluationService {

    private final UserService userService;
    private final ProjectManager projectManager;
    private final JobMapper jobMapper;
    private final ViewConfigMapper viewConfigMapper;
    private final IdConvertor idConvertor;
    private final ViewConfigConvertor viewConfigConvertor;
    private final JobConvertor jobConvertor;
    private final JobStatusMachine jobStatusMachine;

    private static final Map<Long, SummaryVo> summaryCache = new ConcurrentHashMap<>();

    public EvaluationService(UserService userService, ProjectManager projectManager, JobMapper jobMapper,
            ViewConfigMapper viewConfigMapper, IdConvertor idConvertor, ViewConfigConvertor viewConfigConvertor,
            JobConvertor jobConvertor, JobStatusMachine jobStatusMachine) {
        this.userService = userService;
        this.projectManager = projectManager;
        this.jobMapper = jobMapper;
        this.viewConfigMapper = viewConfigMapper;
        this.idConvertor = idConvertor;
        this.viewConfigConvertor = viewConfigConvertor;
        this.jobConvertor = jobConvertor;
        this.jobStatusMachine = jobStatusMachine;
    }


    public List<AttributeVo> listAttributeVo() {
        List<String> attributes = FileUtil.readLines(
                Objects.requireNonNull(this.getClass().getResource("/config/evaluation_attributes")),
                Charset.defaultCharset());

        return attributes.stream().map(line -> {
            String[] arr = line.split(",");
            return AttributeVo.builder().name(arr[0]).type(arr[1]).build();
        }).collect(Collectors.toList());
    }

    public ConfigVo getViewConfig(ConfigQuery configQuery) {
        Long userId = userService.currentUserDetail().getId();
        Long projectId = projectManager.getProjectId(configQuery.getProjectUrl());

        ViewConfigEntity viewConfig = viewConfigMapper.findViewConfig(userId, projectId,
                configQuery.getName());
        if (viewConfig == null) {
            return null;
        }
        return viewConfigConvertor.convert(viewConfig);
    }

    public Boolean createViewConfig(String projectUrl, ConfigRequest configRequest) {
        Long userId = userService.currentUserDetail().getId();
        Long projectId = projectManager.getProjectId(projectUrl);
        ViewConfigEntity entity = ViewConfigEntity.builder()
                .ownerId(userId)
                .projectId(projectId)
                .configName(configRequest.getName())
                .content(configRequest.getContent())
                .build();
        int res = viewConfigMapper.createViewConfig(entity);
        return res > 0;
    }

    public PageInfo<SummaryVo> listEvaluationSummary(String projectUrl,
            SummaryFilter summaryFilter, PageParams pageParams) {
        PageHelper.startPage(pageParams.getPageNum(), pageParams.getPageSize());
        Long projectId = projectManager.getProjectId(projectUrl);
        List<JobEntity> jobEntities = jobMapper.listJobs(projectId, null);
        return PageUtil.toPageInfo(jobEntities, this::toSummary);
    }


    private SummaryVo toSummary(JobEntity entity) {
        if (summaryCache.containsKey(entity.getId())) {
            return summaryCache.get(entity.getId());
        }

        JobVo jobVo = jobConvertor.convert(entity);
        SummaryVo summaryVo = SummaryVo.builder()
                .id(jobVo.getId())
                .uuid(jobVo.getUuid())
                .projectId(idConvertor.convert(entity.getProject().getId()))
                .projectName(entity.getProject().getProjectName())
                .modelName(jobVo.getModelName())
                .modelVersion(jobVo.getModelVersion())
                .datasets(StrUtil.join(",", jobVo.getDatasets()))
                .runtime(jobVo.getRuntime().getName())
                .device(jobVo.getDevice())
                .deviceAmount(jobVo.getDeviceAmount())
                .createdTime(jobVo.getCreatedTime())
                .stopTime(jobVo.getStopTime())
                .owner(jobVo.getOwner().getName())
                .duration(jobVo.getDuration())
                .attributes(Lists.newArrayList())
                .build();
        // TODO:remove all of these implements
        /*Map<String, Object> result = resultQuerier.flattenResultOfJob(entity.getId());
        for (Entry<String, Object> entry : result.entrySet()) {
            String value = String.valueOf(entry.getValue());
            summaryVo.getAttributes().add(AttributeValueVo.builder()
                .name(entry.getKey())
                .type(getAttributeType(value))
                .value(value)
                .build());
        }*/

        // only cache the jobs which have the final status
        if (jobStatusMachine.isFinal(entity.getJobStatus())) {
            summaryCache.put(entity.getId(), summaryVo);
        }
        return summaryVo;
    }

    private String getAttributeType(String value) {
        if (NumberUtil.isInteger(value)) {
            return "int";
        }
        if (NumberUtil.isDouble(value)) {
            return "float";
        }
        return "string";
    }
}
