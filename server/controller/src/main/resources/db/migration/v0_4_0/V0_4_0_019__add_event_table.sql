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

create table event
(
    id           bigint auto_increment primary key,
    type         varchar(255)                       not null comment 'enum of info, warn, error',
    source       varchar(255)                       not null comment 'event source, e.g. server, client',
    resource     varchar(255)                       not null comment 'the related resource, e.g. job, task ',
    resource_id  bigint                             not null,
    message      varchar(255)                       not null comment 'message string',
    data         json                               null comment 'extra data with json fmt, optional',
    created_time datetime default CURRENT_TIMESTAMP not null
);

create index event_resource_index
    on event (resource, resource_id);
