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

import io.kubernetes.client.informer.ResourceEventHandler;
import io.kubernetes.client.openapi.models.V1Pod;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class PodEventHandler implements ResourceEventHandler<V1Pod> {


    public PodEventHandler() {

    }

    @Override
    public void onAdd(V1Pod obj) {
    }

    @Override
    public void onUpdate(V1Pod oldObj, V1Pod newObj) {
        newObj.getStatus().getPhase();

    }

    @Override
    public void onDelete(V1Pod obj, boolean deletedFinalStateUnknown) {
    }
}
