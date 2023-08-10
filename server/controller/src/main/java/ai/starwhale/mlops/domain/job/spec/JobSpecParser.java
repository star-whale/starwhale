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

package ai.starwhale.mlops.domain.job.spec;

import ai.starwhale.mlops.api.protobuf.Model.StepSpec;
import ai.starwhale.mlops.api.protobuf.Model.StepSpec.Builder;
import ai.starwhale.mlops.common.Constants;
import ai.starwhale.mlops.exception.SwValidationException;
import ai.starwhale.mlops.exception.SwValidationException.ValidSubject;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectWriter;
import com.google.protobuf.InvalidProtocolBufferException;
import com.google.protobuf.util.JsonFormat;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import org.springframework.lang.Nullable;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;

@Slf4j
@Component
public class JobSpecParser {

    public List<StepSpec> parseAndFlattenStepFromYaml(String yamlContent) throws JsonProcessingException {
        try {
            List<Object> map = Constants.yamlMapper.readValue(yamlContent, new TypeReference<>() {
            });
            return map.stream()
                    .map(this::objectToStepSpecBuilder)
                    .filter(Objects::nonNull)
                    .map(Builder::build)
                    .collect(Collectors.toList());

        } catch (JsonProcessingException e) {
            return parseAllStepFromYaml(yamlContent);
        }
    }

    public List<StepSpec> parseAllStepFromYaml(String yamlContent) throws JsonProcessingException {
        return parseStepFromYaml(yamlContent, null);
    }

    public List<StepSpec> parseStepFromYaml(String yamlContent, String jobName) throws JsonProcessingException {
        Map<String, List<Object>> map = Constants.yamlMapper.readValue(yamlContent, new TypeReference<>() {
        });
        Map<String, List<StepSpec>> ret = new HashMap<>();
        // update job name for each step spec
        map.forEach((k, v) -> {
            if (jobName != null && !k.equals(jobName)) {
                return;
            }
            List<StepSpec> specList = v.stream()
                    .map(item -> {
                        var builder = objectToStepSpecBuilder(item);
                        if (builder != null) {
                            builder.setJobName(k);
                            return builder.build();
                        }
                        return null;
                    })
                    .filter(Objects::nonNull)
                    .collect(Collectors.toList());
            ret.put(k, specList);
        });
        List<StepSpec> specList = ret.values().stream().flatMap(Collection::stream).collect(Collectors.toList());
        if (CollectionUtils.isEmpty(specList)) {
            log.error("step specification is empty for {}", yamlContent);
            throw new SwValidationException(ValidSubject.MODEL);
        }
        return specList;
    }

    public String stepToYaml(List<StepSpec> stepSpecs) {
        // TODO: refine this ugly code (jialei)
        var jsonItems = stepSpecs.stream().map(this::stepToJsonQuietly).collect(Collectors.toList());
        // join json items to json array string
        var json = "[" + String.join(",", jsonItems) + "]";
        try {
            var map = Constants.yamlMapper.readValue(json, new TypeReference<>() {
            });
            return Constants.yamlMapper.writeValueAsString(map);
        } catch (JsonProcessingException e) {
            log.error("failed to convert step spec to yaml string {}", stepSpecs, e);
        }
        return null;
    }

    public String stepToJsonQuietly(StepSpec stepSpec) {
        try {
            return JsonFormat.printer().print(stepSpec);
        } catch (InvalidProtocolBufferException e) {
            log.error("failed to convert step spec to json string {}", stepSpec, e);
        }
        return null;
    }

    public StepSpec stepFromJsonQuietly(@Nullable String json) {
        // for backward compatibility
        if (json == null) {
            return null;
        }

        try {
            var builder = StepSpec.newBuilder();
            JsonFormat.parser().ignoringUnknownFields().merge(json, builder);
            return builder.build();
        } catch (InvalidProtocolBufferException e) {
            log.error("failed to convert json string to step spec {}", json, e);
        }
        return null;
    }

    private Builder objectToStepSpecBuilder(Object item) {
        try {
            var builder = StepSpec.newBuilder();
            // TODO: refine this ugly code (jialei)
            ObjectWriter ow = new ObjectMapper().writer().withDefaultPrettyPrinter();
            String json = ow.writeValueAsString(item);
            JsonFormat.parser().ignoringUnknownFields().merge(json, builder);
            if (!builder.hasReplicas()) {
                builder.setReplicas(1);
            }
            return builder;
        } catch (Exception e) {
            log.error("failed to parse step spec {}", item, e);
            return null;
        }
    }
}
