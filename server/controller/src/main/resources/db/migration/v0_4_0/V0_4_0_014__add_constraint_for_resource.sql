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


ALTER TABLE model_info ADD deleted_time BIGINT DEFAULT 0 NOT NULL;
ALTER TABLE runtime_info ADD deleted_time BIGINT DEFAULT 0 NOT NULL;
ALTER TABLE dataset_info ADD deleted_time BIGINT DEFAULT 0 NOT NULL;

UPDATE model_info set deleted_time = UNIX_TIMESTAMP(modified_time) * 1000 where is_deleted != 0;
UPDATE runtime_info set deleted_time = UNIX_TIMESTAMP(modified_time) * 1000 where is_deleted != 0;
UPDATE dataset_info set deleted_time = UNIX_TIMESTAMP(modified_time) * 1000 where is_deleted != 0;

ALTER TABLE model_info ADD CONSTRAINT model_info_UK UNIQUE KEY (model_name,project_id,deleted_time);
ALTER TABLE runtime_info ADD CONSTRAINT runtime_info_UK UNIQUE KEY (runtime_name,project_id,deleted_time);
ALTER TABLE dataset_info ADD CONSTRAINT dataset_info_UK UNIQUE KEY (dataset_name,project_id,deleted_time);

ALTER TABLE model_info DROP COLUMN is_deleted;
ALTER TABLE runtime_info DROP COLUMN is_deleted;
ALTER TABLE dataset_info DROP COLUMN is_deleted;
