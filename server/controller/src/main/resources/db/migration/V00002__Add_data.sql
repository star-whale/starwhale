/*
 * Copyright 2022.1-2022
 * StarWhale.ai All right reserved. This software is the confidential and proprietary information of
 * StarWhale.ai ("Confidential Information"). You shall not disclose such Confidential Information and shall use it only
 * in accordance with the terms of the license agreement you entered into with StarWhale.ai.
 */

#init data for test

insert into user_role(role_name, role_name_en)
values ('admin', 'admin');
insert into user_role(role_name, role_name_en)
values ('user', 'user');

#password=abcd1234
insert into user_info(user_name, user_pwd, user_pwd_salt, role_id, user_enabled)
values ('starwhale', 'ee9533077d01d2d65a4efdb41129a91e', '6ea18d595773ccc2beacce26', 1, 1);

#password=asdf7890
insert into user_info(user_name, user_pwd, user_pwd_salt, role_id, user_enabled)
values ('test', '7ce1c1d60c3393e4ca681e738036fe8c', 'be1866739033b7907631a71e', 1, 0);

insert into project_info(project_name, owner_id)
values ('project_for_test1', 1);
insert into project_info(project_name, owner_id)
values ('project_for_test2', 2);

insert into agent_info(agent_ip, connect_time, agent_version, device_info)
values (inet_aton('192.168.1.1'), '2022-03-20 15:00:30', 'v1.0.0', '{}');
insert into agent_info(agent_ip, connect_time, agent_version, device_info)
values (inet_aton('192.168.1.2'), '2022-03-20 15:10:32', 'v1.0.0', '{}');
insert into agent_info(agent_ip, connect_time, agent_version, device_info)
values (inet_aton('192.168.1.3'), '2022-03-21 15:10:32', 'v1.1.0', '{}');

