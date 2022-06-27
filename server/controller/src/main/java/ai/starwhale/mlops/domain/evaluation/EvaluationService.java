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

import ai.starwhale.mlops.api.protocol.evaluation.AttributeVO;
import ai.starwhale.mlops.api.protocol.evaluation.AttributeValueVO;
import ai.starwhale.mlops.api.protocol.evaluation.ConfigRequest;
import ai.starwhale.mlops.api.protocol.evaluation.ConfigVO;
import ai.starwhale.mlops.api.protocol.evaluation.SummaryVO;
import ai.starwhale.mlops.api.protocol.job.JobVO;
import ai.starwhale.mlops.common.IDConvertor;
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
import ai.starwhale.mlops.domain.project.ProjectManager;
import ai.starwhale.mlops.domain.user.UserService;
import ai.starwhale.mlops.resulting.ResultQuerier;
import cn.hutool.core.io.FileUtil;
import cn.hutool.core.util.NumberUtil;
import cn.hutool.core.util.StrUtil;
import com.github.pagehelper.PageHelper;
import com.github.pagehelper.PageInfo;
import com.google.common.collect.Lists;
import java.nio.charset.Charset;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.stream.Collectors;
import javax.annotation.Resource;
import org.springframework.stereotype.Service;

@Service
public class EvaluationService {

    @Resource
    private UserService userService;

    @Resource
    private ProjectManager projectManager;

    @Resource
    private ViewConfigMapper viewConfigMapper;

    @Resource
    private ViewConfigConvertor viewConfigConvertor;

    @Resource
    private JobMapper jobMapper;

    @Resource
    private JobConvertor jobConvertor;

    @Resource
    private IDConvertor idConvertor;

    @Resource
    private ResultQuerier resultQuerier;

    public List<AttributeVO> listAttributeVO() {
        List<String> attributes = FileUtil.readLines(
            Objects.requireNonNull(this.getClass().getResource("/config/evaluation_attributes")),
            Charset.defaultCharset());

        return attributes.stream().map(line -> {
            String[] arr = line.split(",");
            return AttributeVO.builder().name(arr[0]).type(arr[1]).build();
        }).collect(Collectors.toList());
    }

    public ConfigVO getViewConfig(ConfigQuery configQuery) {
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

    public PageInfo<SummaryVO> listEvaluationSummary(String projectUrl,
        SummaryFilter summaryFilter, PageParams pageParams) {
        PageHelper.startPage(pageParams.getPageNum(), pageParams.getPageSize());
        Long projectId = projectManager.getProjectId(projectUrl);
        List<JobEntity> jobEntities = jobMapper.listJobsByStatus(projectId, null,
            JobStatus.SUCCESS);
        return PageUtil.toPageInfo(jobEntities, this::toSummary);
    }

    private SummaryVO toSummary(JobEntity entity) {

        JobVO jobVO = jobConvertor.convert(entity);
        SummaryVO summaryVO = SummaryVO.builder()
            .id(jobVO.getId())
            .uuid(jobVO.getUuid())
            .projectId(idConvertor.convert(entity.getProject().getId()))
            .projectName(entity.getProject().getProjectName())
            .modelName(jobVO.getModelName())
            .modelVersion(jobVO.getModelVersion())
            .datasets(StrUtil.join(",", jobVO.getDatasets()))
            .runtime(jobVO.getRuntime().getName())
            .device(jobVO.getDevice())
            .deviceAmount(jobVO.getDeviceAmount())
            .createdTime(jobVO.getCreatedTime())
            .stopTime(jobVO.getStopTime())
            .owner(jobVO.getOwner().getName())
            .duration(jobVO.getDuration())
            .attributes(Lists.newArrayList())
            .build();
        Map<String, Object> result = resultQuerier.flattenSummaryOfJob(entity.getId());
        for (Entry<String, Object> entry : result.entrySet()) {
            String value = String.valueOf(entry.getValue());
            summaryVO.getAttributes().add(AttributeValueVO.builder()
                .name(entry.getKey())
                .type(getAttributeType(value))
                .value(value)
                .build());
        }
        return summaryVO;
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
