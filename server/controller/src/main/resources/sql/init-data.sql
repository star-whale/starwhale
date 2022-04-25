/*
 * Copyright 2022.1-2022
 * StarWhale.ai All right reserved. This software is the confidential and proprietary information of
 * StarWhale.ai ("Confidential Information"). You shall not disclose such Confidential Information and shall use it only
 * in accordance with the terms of the license agreement you entered into with StarWhale.ai.
 */

#init data for test

insert into user_role(role_name, role_name_en)
select 'admin', 'admin' from dual
where not exists (select id from user_role where role_name='admin');

insert into user_role(role_name, role_name_en)
select 'user', 'user' from dual
where not exists (select id from user_role where role_name='user');


#password=abcd1234
insert into user_info(user_name, user_pwd, user_pwd_salt, role_id, user_enabled)
select 'starwhale', 'ee9533077d01d2d65a4efdb41129a91e', '6ea18d595773ccc2beacce26', 1, 1 from dual
where not exists (select id from user_info where user_name='starwhale');

#password=asdf7890
insert into user_info(user_name, user_pwd, user_pwd_salt, role_id, user_enabled)
select 'test', '7ce1c1d60c3393e4ca681e738036fe8c', 'be1866739033b7907631a71e', 1, 0 from dual
where not exists (select id from user_info where user_name='test');


