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

package ai.starwhale.mlops.schedule.impl.k8s;

import static ai.starwhale.mlops.schedule.impl.k8s.K8sSwTaskScheduler.ANNOTATION_KEY_TASK_GENERATION;

import io.kubernetes.client.openapi.models.V1ObjectMeta;
import java.time.OffsetDateTime;
import lombok.extern.slf4j.Slf4j;
import org.springframework.util.StringUtils;

@Slf4j
public class Util {
    public static Long k8sTimeToMs(OffsetDateTime time) {
        if (time == null) {
            return null;
        }
        return time.toInstant().toEpochMilli();
    }

    /**
     * Retrieves the task generation from the given V1ObjectMeta.
     *
     * @param meta the V1ObjectMeta from which to retrieve the task generation
     * @return the task generation as a Long, or null if it cannot be retrieved
     */
    public static Long getTaskGeneration(V1ObjectMeta meta) {
        if (meta == null) {
            return null;
        }
        var annotations = meta.getAnnotations();
        if (annotations == null) {
            return null;
        }
        var str = annotations.get(ANNOTATION_KEY_TASK_GENERATION);
        if (!StringUtils.hasText(str)) {
            return null;
        }

        try {
            return Long.parseLong(str);
        } catch (Exception e) {
            log.warn("task generation is not a number {}", str);
            return null;
        }
    }
}
