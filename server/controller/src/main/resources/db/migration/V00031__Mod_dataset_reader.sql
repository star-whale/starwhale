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
TRUNCATE TABLE dataset_read_session;
ALTER TABLE dataset_read_session MODIFY COLUMN id BIGINT auto_increment NOT NULL COMMENT 'PK';
ALTER TABLE dataset_read_session ADD session_id varchar(255) NOT NULL;
ALTER TABLE dataset_read_session ADD CONSTRAINT dataset_read_session_UN UNIQUE KEY (session_id,dataset_name,dataset_version);

TRUNCATE TABLE dataset_read_log;
ALTER TABLE dataset_read_log DROP KEY session_readrange;
ALTER TABLE dataset_read_log MODIFY COLUMN session_id BIGINT NOT NULL;
ALTER TABLE dataset_read_log ADD CONSTRAINT dataset_read_log_UK UNIQUE KEY (session_id,`start`,`end`);
