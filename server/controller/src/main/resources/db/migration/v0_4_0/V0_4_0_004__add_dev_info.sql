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

ALTER TABLE job_info
    ADD dev_password VARCHAR(255) NULL,
    ADD dev_way VARCHAR(255) NULL,
    CHANGE debug_mode dev_mode TINYINT DEFAULT 0 NULL;

ALTER TABLE task_info
    ADD ip VARCHAR(255) NULL,
    ADD dev_password VARCHAR(255) NULL,
    ADD dev_way VARCHAR(255) NULL;
