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

package ai.starwhale.mlops.common.proxy;

import ai.starwhale.mlops.domain.job.ModelServingService;
import ai.starwhale.mlops.domain.job.mapper.ModelServingMapper;
import java.util.Date;
import org.springframework.stereotype.Component;

/**
 * This class is used to proxy the model serving service.
 * The model serving service uri is like "model-serving/1/xxx", the first number is the id of the model serving entry.
 * The proxy will find the target host by the id.
 * The proxy will update the last visit time of the model serving entry which is used to do the garbage collection.
 */
@Component
public class ModelServing implements Service {
    private final ModelServingMapper modelServingMapper;

    public static final String MODEL_SERVICE_PREFIX = "model-serving";

    public ModelServing(ModelServingMapper modelServingMapper) {
        this.modelServingMapper = modelServingMapper;
    }

    @Override
    public String getPrefix() {
        return MODEL_SERVICE_PREFIX;
    }

    @Override
    public String getTarget(String uri) {
        var parts = uri.split("/", 2);

        var id = Long.parseLong(parts[0]);

        if (modelServingMapper.find(id) == null) {
            throw new IllegalArgumentException("can not find model serving entry " + parts[1]);
        }
        modelServingMapper.updateLastVisitTime(id, new Date());

        var svc = ModelServingService.getServiceName(id);
        var handler = "";
        if (parts.length == 2) {
            handler = parts[1];
        }
        return String.format("http://%s/%s", svc, handler);
    }
}
