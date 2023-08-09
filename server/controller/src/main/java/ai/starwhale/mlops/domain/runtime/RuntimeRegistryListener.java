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

package ai.starwhale.mlops.domain.runtime;

import ai.starwhale.mlops.configuration.DockerSetting;
import ai.starwhale.mlops.domain.system.SystemSetting;
import ai.starwhale.mlops.domain.system.SystemSettingListener;
import ai.starwhale.mlops.schedule.impl.k8s.K8sClient;
import cn.hutool.json.JSONUtil;
import io.kubernetes.client.openapi.ApiException;
import io.kubernetes.client.openapi.models.V1ObjectMeta;
import io.kubernetes.client.openapi.models.V1Secret;
import java.util.Map;
import javax.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

@Slf4j
@Component
public class RuntimeRegistryListener implements SystemSettingListener {

    private final K8sClient k8sClient;

    private static final String DOCKER_REGISTRY_SECRET = "regcred";

    public RuntimeRegistryListener(K8sClient k8sClient) {
        this.k8sClient = k8sClient;
    }

    private boolean validateDockerSetting(DockerSetting setting) {
        return null != setting
            && StringUtils.hasText(setting.getRegistryForPull())
            && StringUtils.hasText(setting.getRegistryForPush());
    }

    @Override
    public void onUpdate(SystemSetting systemSetting) {
        if (null != systemSetting && validateDockerSetting(systemSetting.getDockerSetting())) {
            var dockerSetting = systemSetting.getDockerSetting();
            var dockerConfigJson = JSONUtil.toJsonStr(
                    Map.of("auths", Map.of(dockerSetting.getRegistryForPush(), Map.of(
                        "username", dockerSetting.getUserName(),
                        "password", dockerSetting.getPassword()))));
            try {
                var secret = k8sClient.getSecret(DOCKER_REGISTRY_SECRET);
                if (secret != null) {
                    // update
                    secret.stringData(Map.of(".dockerconfigjson", dockerConfigJson));
                    k8sClient.replaceSecret(DOCKER_REGISTRY_SECRET, secret);
                }
            } catch (ApiException e) {
                if (e.getCode() == HttpServletResponse.SC_NOT_FOUND) {
                    // create
                    try {
                        k8sClient.createSecret(new V1Secret()
                                .metadata(new V1ObjectMeta().name(DOCKER_REGISTRY_SECRET))
                                .type("kubernetes.io/dockerconfigjson")
                                .immutable(false)
                                .stringData(Map.of(".dockerconfigjson", dockerConfigJson))
                        );
                    } catch (ApiException ex) {
                        log.error("create secret error: {}", e.getResponseBody(), e);
                    }
                } else {
                    log.error("operate secret error: {}", e.getResponseBody(), e);
                }
            }
        } else {
            // delete docker config
            try {
                var status = k8sClient.deleteSecret(DOCKER_REGISTRY_SECRET);
                log.info("secret:{} delete success, info:{}", DOCKER_REGISTRY_SECRET, status);
            } catch (ApiException e) {
                if (e.getCode() == HttpServletResponse.SC_NOT_FOUND) {
                    log.info("secret:{} not found", DOCKER_REGISTRY_SECRET);
                    return;
                }
                log.error("secret:{} delete error:{}", DOCKER_REGISTRY_SECRET, e.getResponseBody(), e);
            }
        }
    }
}
