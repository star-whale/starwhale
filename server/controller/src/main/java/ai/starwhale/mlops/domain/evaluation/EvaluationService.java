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
import ai.starwhale.mlops.domain.evaluation.bo.ConfigQuery;
import ai.starwhale.mlops.domain.evaluation.mapper.ViewConfigMapper;
import ai.starwhale.mlops.domain.evaluation.po.ViewConfigEntity;
import ai.starwhale.mlops.domain.project.ProjectService;
import ai.starwhale.mlops.domain.user.UserService;
import cn.hutool.core.io.FileUtil;
import java.nio.charset.Charset;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;

@Service
public class EvaluationService {

    public static final String TABLE_NAME_FORMAT = "project/%s/eval/summary";

    private final UserService userService;
    private final ProjectService projectService;
    private final ViewConfigMapper viewConfigMapper;
    private final ViewConfigConverter viewConfigConvertor;

    public EvaluationService(
            UserService userService,
            ProjectService projectService,
            ViewConfigMapper viewConfigMapper,
            ViewConfigConverter viewConfigConvertor
    ) {
        this.userService = userService;
        this.projectService = projectService;
        this.viewConfigMapper = viewConfigMapper;
        this.viewConfigConvertor = viewConfigConvertor;
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
        Long projectId = projectService.getProjectId(configQuery.getProjectUrl());

        ViewConfigEntity viewConfig = viewConfigMapper.findViewConfig(projectId, configQuery.getName());
        if (viewConfig == null) {
            return null;
        }
        return viewConfigConvertor.convert(viewConfig);
    }

    public Boolean createViewConfig(String projectUrl, ConfigRequest configRequest) {
        Long userId = userService.currentUserDetail().getId();
        Long projectId = projectService.getProjectId(projectUrl);
        ViewConfigEntity entity = ViewConfigEntity.builder()
                .ownerId(userId)
                .projectId(projectId)
                .configName(configRequest.getName())
                .content(configRequest.getContent())
                .build();
        int res = viewConfigMapper.createViewConfig(entity);
        return res > 0;
    }
}
