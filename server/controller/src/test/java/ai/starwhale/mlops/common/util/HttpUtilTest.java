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

package ai.starwhale.mlops.common.util;

import ai.starwhale.mlops.common.util.HttpUtil.Resources;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public class HttpUtilTest {

    @Test
    public void testGetResourceUrlFromPath() {
        Assertions.assertEquals("project_test_1",
                HttpUtil.getResourceUrlFromPath("/api/v1/project/project_test_1", Resources.PROJECT));
        Assertions.assertEquals("project_test_1",
                HttpUtil.getResourceUrlFromPath("/api/v1/project/project_test_1/model/1", Resources.PROJECT));
        Assertions.assertEquals("project_test_1",
                HttpUtil.getResourceUrlFromPath("/project/project_test_1?pageSize=1", "project"));
        Assertions.assertNull(
                HttpUtil.getResourceUrlFromPath("/project/project_test_1?pageSize=1", Resources.RUNTIME));
        Assertions.assertEquals("1",
                HttpUtil.getResourceUrlFromPath("/api/v1/project/project_test_1/model/1", "model"));
    }
}
