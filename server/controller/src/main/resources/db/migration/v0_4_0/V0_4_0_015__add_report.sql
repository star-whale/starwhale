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


create table if not exists report
(
    id            bigint auto_increment primary key  not null,
    uuid          varchar(255)          unique key   not null,
    title         varchar(255)                       not null,
    description   varchar(255),
    content       text                               not null,
    project_id    bigint                             not null,
    owner_id      bigint                             not null,
    shared        tinyint  default 0                 not null,
    deleted_time  bigint   default 0                 not null,
    created_time  datetime default CURRENT_TIMESTAMP not null,
    modified_time datetime default CURRENT_TIMESTAMP null on update CURRENT_TIMESTAMP,
    INDEX `idx_title` (`title`) USING BTREE,
    INDEX `idx_project_id` (`project_id`) USING BTREE
);
