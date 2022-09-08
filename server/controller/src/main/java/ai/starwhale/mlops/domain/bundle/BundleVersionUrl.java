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

package ai.starwhale.mlops.domain.bundle;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class BundleVersionUrl {

    private BundleUrl bundleUrl;

    private String versionUrl;


    public static BundleVersionUrl create(BundleUrl bundleUrl, String versionUrl) {
        return new BundleVersionUrl(bundleUrl, versionUrl);
    }

    public static BundleVersionUrl create(String projectUrl, String bundleUrl, String versionUrl) {
        return create(BundleUrl.create(projectUrl, bundleUrl), versionUrl);
    }
}
