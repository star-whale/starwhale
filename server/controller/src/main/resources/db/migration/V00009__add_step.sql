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
truncate table job_info;
ALTER TABLE `job_info`
    ADD COLUMN `job_type` VARCHAR(50) NOT NULL AFTER `job_status`;

truncate table task_info;
ALTER TABLE task_info RENAME COLUMN job_id TO step_id;
TRUNCATE job_dataset_version_rel;
CREATE TABLE IF NOT EXISTS step
(
    id              bigint           NOT NULL AUTO_INCREMENT COMMENT 'PK',
    step_uuid       varchar(255)     NOT NULL,
    step_name       varchar(255)     NOT NULL,
    job_id          bigint           NOT NULL,
    last_step_id    bigint           ,
    step_status     varchar(50)      NOT NULL,
    created_time    datetime         NOT NULL DEFAULT CURRENT_TIMESTAMP,
    modified_time   datetime         NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    UNIQUE INDEX uk_step_uuid (step_uuid) USING BTREE,
    INDEX idx_step_job_id (job_id) USING BTREE
    );