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

CREATE TABLE IF NOT EXISTS runtime_info
(
    id            bigint           NOT NULL AUTO_INCREMENT COMMENT 'PK',
    runtime_name     varchar(255)     NOT NULL,
    project_id    bigint           NOT NULL,
    owner_id      bigint           NOT NULL,
    is_deleted    tinyint UNSIGNED NOT NULL DEFAULT 0,
    created_time  datetime         NOT NULL DEFAULT CURRENT_TIMESTAMP,
    modified_time datetime         NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    INDEX idx_runtime_name (runtime_name) USING BTREE,
    INDEX idx_project_id (project_id) USING BTREE,
    INDEX idx_owner_id (owner_id) USING BTREE
);

CREATE TABLE IF NOT EXISTS runtime_version
(
    id            bigint       NOT NULL AUTO_INCREMENT COMMENT 'PK',
    runtime_id    bigint       NOT NULL,
    owner_id      bigint       NOT NULL,
    version_name  varchar(255) NOT NULL,
    version_tag   varchar(255) ,
    version_meta  TEXT         NOT NULL,
    storage_path  TEXT         NOT NULL,
    created_time  datetime     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    modified_time datetime     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    INDEX idx_runtime_id (runtime_id) USING BTREE,
    INDEX idx_owner_id (owner_id) USING BTREE,
    unique unq_runtime_version_name (runtime_id,version_name) USING BTREE
);