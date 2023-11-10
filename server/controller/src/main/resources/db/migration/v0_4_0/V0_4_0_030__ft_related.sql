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

CREATE TABLE IF NOT EXISTS `fine_tune_space`
(
    `id`            bigint       NOT NULL AUTO_INCREMENT COMMENT 'PK',
    `project_id`      bigint NOT NULL,
    `owner_id`  bigint     NOT NULL,
    `name` varchar(32) NOT NULL,
    `description`  varchar(255)  NOT NULL,
    `created_time`  datetime     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `modified_time` datetime     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`) USING BTREE,
    UNIQUE INDEX `uk_ft_space_project` (`project_id`, `name`) USING BTREE
);

CREATE TABLE IF NOT EXISTS `fine_tune`
(
    `id`                        bigint       NOT NULL AUTO_INCREMENT COMMENT 'PK',
    `space_id`                   bigint NOT NULL,
    `job_id`                     bigint     NOT NULL,
    `eval_datasets`              JSON  NULL,
    `train_datasets`             JSON  NULL,
    `base_model_version_id`        bigint NOT NULL,
    `target_model_version_id`      bigint NULL,
    `created_time`              datetime     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `modified_time`             datetime     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`) USING BTREE,
    UNIQUE INDEX `idx_ft_job` (`job_id`) USING BTREE,
    INDEX `idx_ft_space` (`space_id`, `created_time`) USING BTREE
);
