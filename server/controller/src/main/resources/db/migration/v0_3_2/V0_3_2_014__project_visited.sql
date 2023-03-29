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
CREATE TABLE IF NOT EXISTS `project_visited`
(
    `id`           bigint   NOT NULL AUTO_INCREMENT COMMENT 'PK',
    `user_id`      bigint   NOT NULL,
    `project_id`   bigint   NOT NULL,
    `visited_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`) USING BTREE,
    UNIQUE INDEX `unq_user_project` (`user_id`, `project_id`) USING BTREE,
    INDEX `idx_visited_time` (`visited_time`) USING BTREE
);
