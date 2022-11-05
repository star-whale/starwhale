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

create table if not exists `dataset_read_session`
(
    `id`                varchar(255)     NOT NULL,
    `dataset_name`      varchar(255)     NOT NULL,
    `dataset_version`   varchar(255)     NOT NULL,
    `table_name`        varchar(255)     NOT NULL,
    `start`             varchar(255)     NULL,
    `start_inclusive`   tinyint unsigned NOT NULL DEFAULT '1',
    `end`               varchar(255)     NULL,
    `end_inclusive`     tinyint unsigned NOT NULL DEFAULT '1',
    `batch_size`        bigint           NOT NULL,
    `created_time`      datetime         NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`)
);

create table if not exists `dataset_read_log`
(
    `id`                bigint           NOT NULL AUTO_INCREMENT COMMENT 'PK',
    `session_id`        varchar(255)     NOT NULL,
    `consumer_id`       varchar(255)     NULL,
    `start`             varchar(255)     NOT NULL,
    `start_inclusive`   tinyint unsigned NOT NULL DEFAULT '1',
    `end`               varchar(255)     NULL,
    `end_inclusive`     tinyint unsigned NOT NULL DEFAULT '0',
    `status`            varchar(20)      NOT NULL,
    `size`              bigint           NOT NULL,
    `assigned_num`      bigint           NOT NULL DEFAULT 0,
    `assigned_time`     datetime         NULL,
    `finished_time`     datetime         NULL,
    `created_time`      datetime         NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `modified_time`     datetime         NOT NULL DEFAULT CURRENT_TIMESTAMP on update CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    UNIQUE KEY `session_readrange` (`session_id`, `start`, `end`)
)
