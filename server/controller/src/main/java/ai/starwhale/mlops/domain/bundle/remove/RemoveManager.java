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

package ai.starwhale.mlops.domain.bundle.remove;

import ai.starwhale.mlops.domain.bundle.BundleManager;
import ai.starwhale.mlops.domain.bundle.BundleUrl;

public class RemoveManager {

    private final BundleManager bundleManager;

    private final RemoveAccessor removeAccessor;

    public static RemoveManager create(BundleManager bundleManager, RemoveAccessor removeAccessor) {
        return new RemoveManager(bundleManager, removeAccessor);
    }

    private RemoveManager(BundleManager bundleManager, RemoveAccessor removeAccessor) {
        this.bundleManager = bundleManager;
        this.removeAccessor = removeAccessor;
    }

    public Boolean removeBundle(BundleUrl bundleUrl) {
        Long id = bundleManager.getBundleId(bundleUrl);
        return removeAccessor.remove(id);
    }
}
