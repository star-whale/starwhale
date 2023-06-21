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

package ai.starwhale.mlops.common.proxy;

public interface Service {
    /**
     * get the fixed uri prefix of the service, this prefix will be used to identify the service
     *
     * @return uri prefix, e.g. "model-serving"
     */
    String getPrefix();

    /**
     * get the target http host from the uri without prefix
     *
     * @param uri uri without prefix
     * @return target http host, e.g. <a href="http://localhost:8080" />
     */
    String getTarget(String uri);
}
