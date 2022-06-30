-- ----------------------------
-- Table structure for dataset_info
-- ----------------------------
create table if not exists view_config
(
    id            bigint auto_increment comment 'PK' primary key,
    config_name   varchar(255)                               not null,
    project_id    bigint                                     not null,
    owner_id      bigint                                     not null,
    content       text                                       not null,
    created_time  datetime         default CURRENT_TIMESTAMP not null,
    modified_time datetime         default CURRENT_TIMESTAMP not null on update CURRENT_TIMESTAMP,
    constraint uk_config_name
        unique (config_name, owner_id, project_id)
);