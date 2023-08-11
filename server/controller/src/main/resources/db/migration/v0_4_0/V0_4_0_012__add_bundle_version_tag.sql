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

create table if not exists bundle_version_tag
(
    id           bigint auto_increment primary key  not null,
    type         varchar(255)                       not null comment 'bundle type e.g. MODEL, DATASET, RUNTIME, JOB',
    bundle_id    bigint                             not null comment 'the id of the bundle',
    tag          varchar(255)                       not null,
    version_id   bigint                             not null comment 'the id of the bundle version',
    created_time datetime default CURRENT_TIMESTAMP not null,
    owner_id     bigint                             not null,
    constraint bundle_version_tag_uk
        unique (type, bundle_id, tag)
);

create index bundle_version_tag_version_id_index
    on bundle_version_tag (version_id);
