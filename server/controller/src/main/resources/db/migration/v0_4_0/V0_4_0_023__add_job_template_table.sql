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

CREATE TABLE IF NOT EXISTS `job_template`
(
    id              bigint  AUTO_INCREMENT PRIMARY KEY COMMENT 'PK',
    name            varchar(255)                        NOT NULL,
    job_id          bigint                              NOT NULL,
    project_id      bigint                              NOT NULL,
    owner_id        bigint                              NOT NULL,
    created_time    datetime DEFAULT CURRENT_TIMESTAMP  NOT NULL,
    modified_time   datetime DEFAULT CURRENT_TIMESTAMP  NULL ON UPDATE CURRENT_TIMESTAMP,
    UNIQUE INDEX `project_name_UK` (`name`, `project_id`) USING BTREE
);
