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

public interface RuntimeTestConstants {
    String BUILTIN_REPO = "homepage.intra.starwhale.ai:5000";
    String BUILTIN_IMAGE = String.format("%s/starwhale:0.4.7.builtin", BUILTIN_REPO);
    String CUSTOM_REPO = "homepage.intra.starwhale.cn";
    String CUSTOM_IMAGE = String.format("%s/starwhale:0.4.7.custom", CUSTOM_REPO);

    String MANIFEST_WITH_BUILTIN_IMAGE = "artifacts:\n"
            + "  dependencies:\n"
            + "  - dependencies/requirements.txt\n"
            + "  - dependencies/.starwhale/lock/requirements-sw-lock.txt\n"
            + "  files: []\n"
            + "  runtime_yaml: runtime.yaml\n"
            + "  wheels: []\n"
            + "base_image: " + BUILTIN_IMAGE + "\n"
            + "build:\n"
            + "  os: Linux\n"
            + "  sw_version: 0.4.7.dev609150619\n"
            + "configs:\n"
            + "  conda:\n"
            + "    channels:\n"
            + "    - conda-forge\n"
            + "    condarc: {}\n"
            + "  docker:\n"
            + "    image: ''\n"
            + "  pip:\n"
            + "    extra_index_url:\n"
            + "    - ''\n"
            + "    index_url: ''\n"
            + "    trusted_host:\n"
            + "    - ''\n"
            + "created_at: 2023-06-12 00:17:11 CST\n"
            + "docker:\n"
            + "  builtin_run_image:\n"
            + "    fullname: " + BUILTIN_IMAGE + "\n"
            + "    name: starwhale\n"
            + "    repo: " + BUILTIN_REPO + "\n"
            + "    tag: 0.4.7.builtin\n"
            + "  custom_run_image: ''\n"
            + "version: m3hxue5f6nie7r36i6pv6cdl6jzxmiz7ptzesudw\n";

    String MANIFEST_WITHOUT_BUILTIN_IMAGE = "artifacts:\n"
            + "  dependencies:\n"
            + "  - dependencies/requirements.txt\n"
            + "  - dependencies/.starwhale/lock/requirements-sw-lock.txt\n"
            + "  files: []\n"
            + "  runtime_yaml: runtime.yaml\n"
            + "  wheels: []\n"
            + "base_image: " + CUSTOM_IMAGE + "\n"
            + "build:\n"
            + "  os: Linux\n"
            + "  sw_version: 0.4.7.dev609150619\n"
            + "configs:\n"
            + "  conda:\n"
            + "    channels:\n"
            + "    - conda-forge\n"
            + "    condarc: {}\n"
            + "  docker:\n"
            + "    image: ''\n"
            + "  pip:\n"
            + "    extra_index_url:\n"
            + "    - ''\n"
            + "    index_url: ''\n"
            + "    trusted_host:\n"
            + "    - ''\n"
            + "created_at: 2023-06-12 00:17:11 CST\n"
            + "docker:\n"
            + "  builtin_run_image:\n"
            + "    fullname: " + BUILTIN_IMAGE + "\n"
            + "    name: starwhale\n"
            + "    repo: " + CUSTOM_REPO + "\n"
            + "    tag: 0.4.7.builtin\n"
            + "  custom_run_image: '" + CUSTOM_IMAGE + "'\n"
            + "version: m3hxue5f6nie7r36i6pv6cdl6jzxmiz7ptzesudw\n";
}
