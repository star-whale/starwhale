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

insert into user_role_rel(user_id, role_id, project_id)
values ((select id from user_info where user_name = 'test'),
        (select id from user_role_info where role_name = 'maintainer'),
        0);

insert into user_role_rel(user_id, role_id, project_id)
values ((select id from user_info where user_name = 'test'),
        (select id from user_role_info where role_name = 'guest'),
        (select id from project_info where project_name = 'starwhale'));