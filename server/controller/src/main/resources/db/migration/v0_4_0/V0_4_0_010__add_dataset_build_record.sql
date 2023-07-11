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


create table if not exists dataset_build_record
(
    id            bigint auto_increment primary key  not null,
    dataset_id    bigint comment 'existence of dataset, it is a new dataset if it is null',
    project_id    bigint                             not null,
    dataset_name  varchar(255)                       not null comment 'should check dataset name when dataset id is null',
    type          varchar(64)                        not null comment 'image, video, audio, others(json, csv, txt ...etc)',
    status        varchar(32)                        not null comment 'created, building, failed, success',
    storage_path  varchar(255)                       not null,
    format        varchar(255)                       not null comment 'reserve for future use',
    created_time  datetime default CURRENT_TIMESTAMP not null,
    modified_time datetime default CURRENT_TIMESTAMP null on update CURRENT_TIMESTAMP
);
