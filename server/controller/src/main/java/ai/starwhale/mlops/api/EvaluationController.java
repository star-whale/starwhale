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

package ai.starwhale.mlops.api;

import ai.starwhale.mlops.api.protocol.Code;
import ai.starwhale.mlops.api.protocol.ResponseMessage;
import ai.starwhale.mlops.api.protocol.evaluation.AttributeVO;
import ai.starwhale.mlops.api.protocol.evaluation.ConfigRequest;
import ai.starwhale.mlops.api.protocol.evaluation.ConfigVO;
import ai.starwhale.mlops.api.protocol.evaluation.SummaryVO;
import ai.starwhale.mlops.common.PageParams;
import ai.starwhale.mlops.domain.evaluation.EvaluationService;
import ai.starwhale.mlops.domain.evaluation.bo.ConfigQuery;
import ai.starwhale.mlops.domain.evaluation.bo.SummaryFilter;
import ai.starwhale.mlops.exception.SWProcessException;
import ai.starwhale.mlops.exception.SWProcessException.ErrorType;
import ai.starwhale.mlops.exception.api.StarWhaleApiException;
import com.github.pagehelper.PageInfo;
import java.util.List;
import javax.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RestController
@RequestMapping("${sw.controller.apiPrefix}")
public class EvaluationController implements EvaluationApi{

    @Resource
    private EvaluationService evaluationService;

    @Override
    public ResponseEntity<ResponseMessage<List<AttributeVO>>>   listAttributes(String projectUrl) {
        List<AttributeVO> vos = evaluationService.listAttributeVO();
        return ResponseEntity.ok(Code.success.asResponse(vos));
    }

    @Override
    public ResponseEntity<ResponseMessage<ConfigVO>> getViewConfig(String projectUrl, String name) {
        ConfigVO viewConfig = evaluationService.getViewConfig(ConfigQuery.builder()
                .projectUrl(projectUrl)
                .name(name)
                .build());
        return ResponseEntity.ok(Code.success.asResponse(viewConfig));
    }

    @Override
    public ResponseEntity<ResponseMessage<String>> createViewConfig(String projectUrl, ConfigRequest configRequest) {
        Boolean res = evaluationService.createViewConfig(projectUrl, configRequest);
        if(!res) {
            throw new StarWhaleApiException(new SWProcessException(ErrorType.DB).tip("Create view config failed."),
                HttpStatus.INTERNAL_SERVER_ERROR);
        }
        return ResponseEntity.ok(Code.success.asResponse("success"));
    }

    @Override
    public ResponseEntity<ResponseMessage<PageInfo<SummaryVO>>> listEvaluationSummary(String projectUrl,
        String filter, Integer pageNum, Integer pageSize) {
        PageInfo<SummaryVO> vos = evaluationService.listEvaluationSummary(
            projectUrl,
            SummaryFilter.parse(filter),
            PageParams.builder()
                .pageNum(pageNum)
                .pageSize(pageSize)
                .build());
        return ResponseEntity.ok(Code.success.asResponse(vos));
    }


}
