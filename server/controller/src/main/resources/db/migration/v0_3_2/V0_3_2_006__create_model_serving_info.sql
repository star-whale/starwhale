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

CREATE TABLE IF NOT EXISTS `model_serving_info`
(
    `id`                 bigint           NOT NULL AUTO_INCREMENT COMMENT 'PK',
    `project_id`         bigint           NOT NULL,
    `model_version_id`   bigint           NOT NULL,
    `owner_id`           bigint           NOT NULL,
    `created_time`       datetime         NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `finished_time`      datetime         NULL     DEFAULT NULL,
    `job_status`         varchar(50)      NOT NULL,
    `runtime_version_id` bigint           NOT NULL,
    `is_deleted`         tinyint UNSIGNED NOT NULL DEFAULT 0,
    `resource_pool`      varchar(255)     NULL     DEFAULT NULL,
    PRIMARY KEY (`id`) USING BTREE,
    INDEX `idx_base_image` (`runtime_version_id`) USING BTREE,
    INDEX `idx_owner_id` (`owner_id`) USING BTREE,
    INDEX `idx_swmp_version_id` (`model_version_id`) USING BTREE
);
