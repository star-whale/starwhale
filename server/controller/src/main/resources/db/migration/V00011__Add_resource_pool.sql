-- ----------------------------
-- Table structure for resource_pool
-- ----------------------------
create table if not exists resource_pool
(
    id            bigint auto_increment comment 'PK' primary key,
    label         varchar(64)                        not null unique,
    name          varchar(255)                       null,
    description   text                               null,
    created_time  datetime default CURRENT_TIMESTAMP not null,
    modified_time datetime default CURRENT_TIMESTAMP not null on update CURRENT_TIMESTAMP
);

insert into resource_pool(label, name, description)
values ('default', 'default', 'default resource pool');

alter table job_info
    add resource_pool_id bigint not null;
