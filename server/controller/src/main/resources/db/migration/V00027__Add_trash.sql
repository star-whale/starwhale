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

-- ----------------------------
-- Table structure for trash
-- ----------------------------
create table if not exists trash
(
    id            bigint auto_increment comment 'PK' primary key,
    project_id    bigint                             not null,
    object_id     bigint                             not null,
    operator_id   bigint                             not null,
    trash_name    varchar(255)                       not null,
    trash_type    varchar(255)                       not null,
    size          bigint   default 0                 not null,
    retention     datetime                           not null,
    updated_time  datetime                           not null,
    created_time  datetime default CURRENT_TIMESTAMP not null,
    modified_time datetime default CURRENT_TIMESTAMP not null on update CURRENT_TIMESTAMP
);
create index idx_project_id on trash (project_id);
create index idx_operator_id on trash (operator_id);