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

package ai.starwhale.mlops.domain.upgrade.step;

import ai.starwhale.mlops.domain.upgrade.UpgradeAccess;
import ai.starwhale.mlops.domain.upgrade.UpgradeService;
import ai.starwhale.mlops.domain.upgrade.bo.Upgrade;
import ai.starwhale.mlops.exception.SwProcessException;
import ai.starwhale.mlops.exception.SwProcessException.ErrorType;
import ai.starwhale.mlops.schedule.impl.k8s.K8sClient;
import cn.hutool.json.JSONUtil;
import io.kubernetes.client.custom.V1Patch;
import io.kubernetes.client.openapi.ApiException;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@Order(2)
public class UpdateK8sImage extends UpgradeStepBase {

    private final K8sClient k8sClient;

    public UpdateK8sImage(UpgradeAccess upgradeAccess, K8sClient k8sClient) {
        super(upgradeAccess);
        this.k8sClient = k8sClient;
    }

    @Override
    protected void doStep(Upgrade upgrade) {
        log.info("Update image" + upgrade.getTo());
        setControllerImage(upgrade.getTo().getImage());
    }

    private void setControllerImage(String image) {
        String patchJson = JSONUtil.toJsonStr(List.of(
                Map.of(
                        "op", "replace",
                        "path", "/spec/template/spec/containers/0/image",
                        "value", Objects.requireNonNull(image)
                )
        ));
        try {
            k8sClient.patchDeployment(
                    "controller",
                    new V1Patch(patchJson),
                    V1Patch.PATCH_FORMAT_JSON_PATCH);
        } catch (ApiException e) {
            throw new SwProcessException(ErrorType.K8S, "Update image error.", e);
        }
    }

    @Override
    public boolean isComplete() {
        try {
            return k8sClient.getNotReadyPods(UpgradeService.LABEL_CONTROLLER).isEmpty();
        } catch (ApiException e) {
            throw new SwProcessException(ErrorType.K8S, "Get pods error.", e);
        }
    }

    @Override
    protected String getTitle() {
        return "Update k8s image.";
    }

    @Override
    protected String getContent() {
        return "";
    }


    @Override
    public void cancel(Upgrade upgrade) {
        log.info("Restore image: " + upgrade.getCurrent());
        setControllerImage(upgrade.getCurrent().getImage());
    }
}
