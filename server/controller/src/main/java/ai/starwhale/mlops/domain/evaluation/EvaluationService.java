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
import ai.starwhale.mlops.api.protocol.evaluation.ConfigRequest;
import ai.starwhale.mlops.api.protocol.evaluation.ConfigVO;
import ai.starwhale.mlops.api.protocol.evaluation.SummaryVO;
import ai.starwhale.mlops.domain.evaluation.bo.ConfigQuery;
import ai.starwhale.mlops.domain.evaluation.bo.SummaryFilter;
import ai.starwhale.mlops.domain.evaluation.mapper.ViewConfigMapper;
import ai.starwhale.mlops.domain.evaluation.po.ViewConfigEntity;
import ai.starwhale.mlops.domain.project.ProjectManager;
import ai.starwhale.mlops.domain.user.UserService;
import cn.hutool.core.io.FileUtil;
import java.nio.charset.Charset;
import java.util.List;
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

    public List<AttributeVO> listAttributeVO() {
        //TODO by liuyunxi
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
        if(viewConfig == null) {
            return null;
        }
        return viewConfigConvertor.convert(viewConfig);
    }

    public Boolean createViewConfig(ConfigRequest configRequest) {
        Long userId = userService.currentUserDetail().getId();
        Long projectId = projectManager.getProjectId(configRequest.getProjectUrl());
        ViewConfigEntity entity = ViewConfigEntity.builder()
            .ownerId(userId)
            .projectId(projectId)
            .configName(configRequest.getName())
            .content(configRequest.getContent())
            .build();
        int res = viewConfigMapper.createViewConfig(entity);
        return res > 0;
    }

    public List<SummaryVO> listEvaluationSummary(SummaryFilter summaryFilter) {
        //TODO by liuyunxi
        return List.of();
    }

}
