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

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import ai.starwhale.mlops.configuration.DockerSetting;
import ai.starwhale.mlops.domain.system.SystemSetting;
import ai.starwhale.mlops.schedule.impl.k8s.K8sClient;
import io.kubernetes.client.openapi.ApiException;
import io.kubernetes.client.openapi.models.V1Secret;
import javax.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;


public class RuntimeRegistryListenerTest {
    private RuntimeRegistryListener listener;
    private K8sClient k8sClient;

    @BeforeEach
    public void setUp() {
        k8sClient = mock(K8sClient.class);
        listener = new RuntimeRegistryListener(k8sClient);
    }


    @Test
    public void testUpdateDockerSettingWithNone() throws ApiException {
        listener.onUpdate(new SystemSetting());
        verify(k8sClient, times(1)).deleteSecret(anyString());
    }

    @Test
    public void testUpdateDockerSettingWithValueAndExistedSecret() throws ApiException {
        given(k8sClient.getSecret(anyString())).willReturn(new V1Secret());
        var setting = new SystemSetting();
        setting.setDockerSetting(new DockerSetting("mockRegistry", "mockRegistry", "admin", "admin", false));
        listener.onUpdate(setting);

        verify(k8sClient, times(1)).replaceSecret(anyString(), any());
    }

    @Test
    public void testUpdateDockerSettingWithValueAndNonExistedSecret() throws ApiException {
        given(k8sClient.getSecret(anyString())).willThrow(new ApiException(HttpServletResponse.SC_NOT_FOUND, ""));
        var setting = new SystemSetting();
        setting.setDockerSetting(new DockerSetting("mockRegistry", "mockRegistry", "admin", "admin", false));
        listener.onUpdate(setting);

        verify(k8sClient, times(1)).createSecret(any());
    }

}
