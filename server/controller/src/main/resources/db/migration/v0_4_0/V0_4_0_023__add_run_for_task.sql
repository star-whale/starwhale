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


create table run
(
    `id`            bigint auto_increment primary key,
    `task_id`       bigint       not null,
    `status`        varchar(30),
    `log_dir`       varchar(255) not null,
    `run_spec`      JSON         not null,
    `ip`            varchar(255),
    `failed_reason` text,
    `start_time`    timestamp(6),
    `finish_time`   timestamp(6),
    `created_time`  datetime     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `modified_time` datetime     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX           `idx_run_task` (`task_id`) USING BTREE
);
