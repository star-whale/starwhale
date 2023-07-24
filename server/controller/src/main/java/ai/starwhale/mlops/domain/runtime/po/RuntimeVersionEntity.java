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

package ai.starwhale.mlops.domain.runtime.po;

import ai.starwhale.mlops.common.BaseEntity;
import ai.starwhale.mlops.common.Constants;
import ai.starwhale.mlops.common.DockerImage;
import ai.starwhale.mlops.domain.bundle.base.BundleVersionEntity;
import ai.starwhale.mlops.domain.runtime.RuntimeService;
import ai.starwhale.mlops.exception.SwValidationException;
import com.fasterxml.jackson.core.JsonProcessingException;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;
import lombok.extern.slf4j.Slf4j;
import org.springframework.util.StringUtils;

@EqualsAndHashCode(callSuper = true)
@Data
@Slf4j
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
public class RuntimeVersionEntity extends BaseEntity implements BundleVersionEntity {

    private Long id;

    private Long versionOrder;

    private Long runtimeId;

    private Long ownerId;

    private String versionName;

    private String versionTag;

    private String versionMeta;
    private RuntimeService.RuntimeManifest versionMetaObj;

    private String storagePath;

    @Deprecated
    private String image;

    private String builtImage;

    @Builder.Default
    private Boolean shared = false;

    @Override
    public String getName() {
        return versionName;
    }

    private static String extractImage(RuntimeService.RuntimeManifest manifest, String replaceableBuiltinRegistry) {
        if (manifest.getDocker() != null) {
            var docker = manifest.getDocker();
            if (StringUtils.hasText(docker.getCustomImage())) {
                return docker.getCustomImage();
            } else {
                var dockerImage = new DockerImage(docker.getBuiltinImage().getFullName());
                return StringUtils.hasText(replaceableBuiltinRegistry)
                        ? dockerImage.resolve(replaceableBuiltinRegistry) : dockerImage.toString();
            }
        } else {
            return manifest.getBaseImage();
        }
    }

    public String getSwVersion() {
        return getVersionMetaObj().getEnvironment().getLock().getSwVersion();
    }

    public String getPythonVersion() {
        return getVersionMetaObj().getEnvironment().getPython();
    }

    public synchronized RuntimeService.RuntimeManifest getVersionMetaObj() {
        if (versionMetaObj != null) {
            return versionMetaObj;
        }
        try {
            versionMetaObj = Constants.yamlMapper.readValue(versionMeta, RuntimeService.RuntimeManifest.class);
        } catch (JsonProcessingException e) {
            log.error("runtime manifest parse error", e);
            throw new SwValidationException(SwValidationException.ValidSubject.RUNTIME, "manifest parse error");
        }
        return versionMetaObj;
    }

    public String getImage() {
        return extractImage(this.getVersionMetaObj(), null);
    }

    public String getImage(String newRegistry) {
        return extractImage(this.getVersionMetaObj(), newRegistry);
    }
}
