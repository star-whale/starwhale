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

create table panel_plugin
(
    id            bigint auto_increment comment 'PK' primary key,
    name          varchar(255)                       not null,
    version       varchar(255)                       not null,
    meta          json                               null,
    storage_path  text                               not null,
    created_time  datetime default CURRENT_TIMESTAMP not null,
    modified_time datetime default CURRENT_TIMESTAMP not null on update CURRENT_TIMESTAMP,
    deleted_time  datetime                           null,
    constraint uniq unique (name, version, deleted_time)
);

create table panel_setting
(
    user_id       bigint                             not null,
    project_id    bigint                             not null,
    name          varchar(255)                       not null,
    content       text                               null,
    created_time  datetime default CURRENT_TIMESTAMP not null,
    modified_time datetime default CURRENT_TIMESTAMP not null on update CURRENT_TIMESTAMP,
    primary key (name, project_id)
);
