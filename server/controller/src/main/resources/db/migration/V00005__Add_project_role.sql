-- ----------------------------
-- Table structure for user_role
-- ----------------------------
create table if not exists user_role_info
(
    id                  bigint auto_increment comment 'PK' primary key,
    role_name           varchar(32) not null,
    role_code           varchar(32) not null,
    role_description    varchar(255) not null,
    constraint uk_role_name
        unique (role_name)
);

insert into user_role_info(role_name, role_code, role_description)
values ('Owner', 'OWNER', '');

insert into user_role_info(role_name, role_code, role_description)
values ('Maintainer', 'MAINTAINER', '');

insert into user_role_info(role_name, role_code, role_description)
values ('Guest', 'GUEST', '');

-- ----------------------------
-- Table structure for user_role_rel
-- ----------------------------
create table if not exists user_role_rel
(
    id                 bigint auto_increment comment 'PK' primary key,
    user_id            bigint                             not null,
    role_id            bigint                             not null,
    project_id         bigint                             not null,
    created_time       datetime default CURRENT_TIMESTAMP not null,
    modified_time      datetime default CURRENT_TIMESTAMP not null on update CURRENT_TIMESTAMP,
    constraint uk_project_role
        unique (user_id, role_id, project_id)
);

create index idx_user_id
    on user_role_rel (user_id);

create index idx_role_id
    on user_role_rel (role_id);

create index idx_project_id
    on user_role_rel (project_id);

insert into user_role_rel(user_id, role_id, project_id)
values ((select id from user_info where user_name='starwhale'),
        (select id from user_role_info where role_name='owner'),
        0);

insert into user_role_rel(user_id, role_id, project_id)
values ((select id from user_info where user_name='starwhale'),
        (select id from user_role_info where role_name='owner'),
        (select id from project_info where project_name='starwhale'));
