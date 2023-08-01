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

package ai.starwhale.mlops.domain.bundle.revert;

import ai.starwhale.mlops.domain.bundle.BundleManager;
import ai.starwhale.mlops.domain.bundle.BundleVersionUrl;
import java.util.Objects;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class RevertManager {

    private final BundleManager bundleManager;

    private final RevertAccessor revertAccessor;

    public static RevertManager create(BundleManager bundleManager, RevertAccessor revertAccessor) {
        return new RevertManager(bundleManager, revertAccessor);
    }

    private RevertManager(BundleManager bundleManager, RevertAccessor revertAccessor) {
        this.bundleManager = bundleManager;
        this.revertAccessor = revertAccessor;
    }

    public Boolean revertVersionTo(BundleVersionUrl bundleVersionUrl) {
        Long bundleId = bundleManager.getBundleId(bundleVersionUrl.getBundleUrl());
        Long versionId = bundleManager.getBundleVersionId(bundleId, bundleVersionUrl.getVersionUrl());

        return revertVersionTo(bundleId, versionId);
    }

    public Boolean revertVersionTo(Long bundleId, Long versionId) {
        Long maxOrder = revertAccessor.selectMaxVersionOrderOfBundleForUpdate(bundleId);
        Long versionOrder = revertAccessor.selectVersionOrderForUpdate(bundleId, versionId);
        if (!Objects.equals(maxOrder, versionOrder) || Objects.equals(maxOrder, 0L)) {
            revertAccessor.updateVersionOrder(versionId, maxOrder + 1);
        }

        return true;
    }
}
