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

create table if not exists server_status
(
    module        varchar(32)                           not null,
    status        varchar(32) default 'NORMAL'          not null,
    progress_uuid varchar(64) default ''                not null,
    modified_time datetime    default CURRENT_TIMESTAMP not null on update CURRENT_TIMESTAMP,
    primary key (module)
);

create table if not exists upgrade_log
(
    id            bigint auto_increment                  not null,
    progress_uuid varchar(64)                            not null,
    step_current  int                                    not null,
    step_total    int                                    not null,
    title         varchar(255)                           not null,
    content       varchar(255) default ''                not null,
    status        varchar(32)                            not null,
    created_time  datetime     default CURRENT_TIMESTAMP not null,
    modified_time datetime     default CURRENT_TIMESTAMP null on update CURRENT_TIMESTAMP,
    primary key (id),
    unique index uniq_uuid_step (progress_uuid, step_current)
);

replace into server_status(module, status, progress_uuid)
values ('CONTROLLER', 'NORMAL', '')
