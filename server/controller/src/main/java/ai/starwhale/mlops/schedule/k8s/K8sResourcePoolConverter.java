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

package ai.starwhale.mlops.schedule.k8s;

import ai.starwhale.mlops.domain.system.resourcepool.bo.ResourcePool;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

/**
 * convert between K8s labels and ResourcePools
 */
@Slf4j
@Component
public class K8sResourcePoolConverter {

    private static final String labelPrefix = "pool.starwhale.ai/";
    private static final String defaultPool = "default";
    private static final String enabledLabelValue = "true";

    /**
     * @param pool ResourcePool
     * @return K8s label which can applied to the node selector
     */
    public Map<String, String> toK8sLabel(ResourcePool pool) {
        var ret = new HashMap<String, String>();

        if (!StringUtils.hasText(pool.getLabel())) {
            return ret;
        }

        // No label need when using default pool.
        if (pool.getLabel().equals(defaultPool)) {
            return ret;
        }

        var key = labelPrefix + pool.getLabel();
        ret.put(key, enabledLabelValue);
        return ret;
    }

    /**
     * @param k8sLabels K8s label to parse
     * @return list of ResourcePool
     */
    public List<ResourcePool> toResourcePools(Map<String, String> k8sLabels) {
        var ret = new ArrayList<ResourcePool>();
        k8sLabels.forEach((key, val) -> {
            // not enabled
            if (!val.equals(enabledLabelValue)) {
                return;
            }
            // invalid key
            if (!key.startsWith(labelPrefix)) {
                return;
            }
            var label = key.substring(labelPrefix.length());
            if (label.isEmpty()) {
                return;
            }
            ret.add(ResourcePool.builder().label(label).build());
        });
        return ret;
    }
}
