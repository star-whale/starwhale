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

package ai.starwhale.mlops.domain.job;

import ai.starwhale.mlops.api.protocol.runtime.RuntimeVersionVo;
import ai.starwhale.mlops.domain.job.mapper.JobMapper;
import ai.starwhale.mlops.domain.job.mapper.ModelServingMapper;
import ai.starwhale.mlops.domain.job.po.JobEntity;
import ai.starwhale.mlops.domain.job.po.ModelServingEntity;
import ai.starwhale.mlops.domain.runtime.converter.RuntimeVersionConverter;
import ai.starwhale.mlops.domain.runtime.mapper.RuntimeVersionMapper;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;

@Service
public class RuntimeSuggestionService {
    private final RuntimeVersionMapper runtimeVersionMapper;
    private final RuntimeVersionConverter runtimeVersionConverter;
    private final JobMapper jobMapper;
    private final ModelServingMapper modelServingMapper;

    public RuntimeSuggestionService(
            RuntimeVersionMapper runtimeVersionMapper,
            RuntimeVersionConverter runtimeVersionConverter,
            JobMapper jobMapper,
            ModelServingMapper modelServingMapper
    ) {
        this.runtimeVersionMapper = runtimeVersionMapper;
        this.runtimeVersionConverter = runtimeVersionConverter;
        this.jobMapper = jobMapper;
        this.modelServingMapper = modelServingMapper;
    }

    /**
     * Get the suggestion runtime list
     * Returns one runtime for now
     * It
     * - returns the latest runtime in the project if the model version is not set
     * - returns the last used runtime if the model version is set (prefer using the online eval version)
     *
     * @param projectId      current project id
     * @param modelVersionId current model version id
     * @return RuntimeVersionVo list
     */
    public List<RuntimeVersionVo> getSuggestions(Long projectId, Long modelVersionId) {
        // get the latest runtime if model not specified
        if (modelVersionId != null) {
            // get the last used runtime in online eval
            var modelServingEntities = modelServingMapper.list(projectId, modelVersionId, null, null);
            if (!modelServingEntities.isEmpty()) {
                modelServingEntities.sort(Comparator.comparing(ModelServingEntity::getLastVisitTime));
                var runtimeVersionId = modelServingEntities.get(modelServingEntities.size() - 1).getRuntimeVersionId();
                return List.of(getByRuntimeVersionId(runtimeVersionId));
            }

            // get the last used runtime in eval jobs
            var jobs = jobMapper.listUserJobs(projectId, modelVersionId);
            var runtimes = jobs.stream().map(JobEntity::getRuntimeVersionId).sorted(Comparator.reverseOrder());
            var runtimeVersionId = runtimes.findFirst().orElse(null);
            if (runtimeVersionId != null) {
                return List.of(getByRuntimeVersionId(runtimeVersionId));
            }
        }

        var entities = runtimeVersionMapper.findLatestByProjectId(projectId, 1);
        return entities.stream().map(runtimeVersionConverter::convert).collect(Collectors.toList());
    }

    private RuntimeVersionVo getByRuntimeVersionId(Long runtimeId) {
        return runtimeVersionConverter.convert(runtimeVersionMapper.find(runtimeId));
    }
}
