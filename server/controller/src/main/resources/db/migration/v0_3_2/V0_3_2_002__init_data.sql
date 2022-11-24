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

#password=abcd1234
insert ignore into user_info(user_name, user_pwd, user_pwd_salt, user_enabled)
values ('starwhale', 'ee9533077d01d2d65a4efdb41129a91e', '6ea18d595773ccc2beacce26', 1);

#password=asdf7890
insert ignore into user_info(user_name, user_pwd, user_pwd_salt, user_enabled)
values ('test', '7ce1c1d60c3393e4ca681e738036fe8c', 'be1866739033b7907631a71e', 0);

insert ignore into project_info(project_name, owner_id)
values ('starwhale', 1);

insert ignore into user_role_info(role_name, role_code, role_description)
values ('Owner', 'OWNER', '');

insert ignore into user_role_info(role_name, role_code, role_description)
values ('Maintainer', 'MAINTAINER', '');

insert ignore into user_role_info(role_name, role_code, role_description)
values ('Guest', 'GUEST', '');

insert ignore into user_role_rel(user_id, role_id, project_id)
values ((select id from user_info where user_name='starwhale'),
        (select id from user_role_info where role_name='owner'),
        0);

insert ignore into user_role_rel(user_id, role_id, project_id)
values ((select id from user_info where user_name='starwhale'),
        (select id from user_role_info where role_name='owner'),
        (select id from project_info where project_name='starwhale'));

insert ignore into user_role_rel(user_id, role_id, project_id)
values ((select id from user_info where user_name = 'test'),
        (select id from user_role_info where role_name = 'maintainer'),
        0);

insert ignore into user_role_rel(user_id, role_id, project_id)
values ((select id from user_info where user_name = 'test'),
        (select id from user_role_info where role_name = 'guest'),
        (select id from project_info where project_name = 'starwhale'));
