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

import io.kubernetes.client.common.KubernetesObject;
import java.time.OffsetDateTime;
import javax.annotation.Nullable;
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

    @Nullable
    public static Long getRunId(@Nullable KubernetesObject object) {
        if (object == null) {
            return null;
        }

        var metadata = object.getMetadata();
        if (metadata == null || metadata.getAnnotations() == null) {
            return null;
        }

        String rid = metadata.getAnnotations().get(RunExecutorK8s.ANNOTATION_KEY_RUN_ID);
        if (!StringUtils.hasText(rid)) {
            return null;
        }
        try {
            return Long.parseLong(rid);
        } catch (NumberFormatException e) {
            log.warn("invalid run id {}", rid);
            return null;
        }
    }
}
