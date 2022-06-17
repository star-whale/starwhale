/*
 * Copyright 2022.1-2022
 * StarWhale.ai All right reserved. This software is the confidential and proprietary information of
 * StarWhale.ai ("Confidential Information"). You shall not disclose such Confidential Information and shall use it only
 * in accordance with the terms of the license agreement you entered into with StarWhale.ai.
 */
CREATE DATABASE IF NOT EXISTS starwhale;
use starwhale;
-- ----------------------------
-- Table structure for agent_info
-- ----------------------------
create table if not exists agent_info
(
    id            bigint auto_increment comment 'PK' primary key,
    agent_ip      int unsigned                       not null,
    connect_time  datetime                           not null,
    agent_version varchar(255)                       not null,
    agent_status  varchar(32)                        not null,
    serial_number varchar(255)                       not null,
    device_info   text                               not null,
    created_time  datetime default CURRENT_TIMESTAMP not null,
    modified_time datetime default CURRENT_TIMESTAMP not null on update CURRENT_TIMESTAMP,
    constraint uk_serial_number
        unique (serial_number)
);
create index idx_agent_version on agent_info (agent_version);

-- ----------------------------
-- Table structure for dag_graph
-- ----------------------------
create table if not exists dag_graph
(
    id            bigint auto_increment comment 'PK'
        primary key,
    job_id        bigint                             not null,
    created_time  datetime default CURRENT_TIMESTAMP not null,
    modified_time datetime default CURRENT_TIMESTAMP not null on update CURRENT_TIMESTAMP,
    constraint idx_dag_job_id
        unique (job_id)
);

-- ----------------------------
-- Table structure for dag_graph_edge
-- ----------------------------
create table if not exists dag_graph_edge
(
    id            bigint auto_increment comment 'PK'
        primary key,
    graph_id      bigint                             not null,
    from_node     bigint                             not null,
    to_node       bigint                             not null,
    content       varchar(255)                       not null,
    created_time  datetime default CURRENT_TIMESTAMP not null,
    modified_time datetime default CURRENT_TIMESTAMP not null on update CURRENT_TIMESTAMP
);

create index idx_dag_edge_from
    on dag_graph_edge (from_node);

create index idx_dag_edge_graph_id
    on dag_graph_edge (graph_id);

create index idx_dag_edge_to
    on dag_graph_edge (to_node);

-- ----------------------------
-- Table structure for dag_graph_node
-- ----------------------------
create table if not exists dag_graph_node
(
    id            bigint auto_increment comment 'PK' primary key,
    graph_id      bigint                             not null,
    node_type     varchar(50)                        not null,
    node_group    varchar(255)                       not null,
    owner_id      bigint                             not null,
    content       varchar(255)                       not null,
    created_time  datetime default CURRENT_TIMESTAMP not null,
    modified_time datetime default CURRENT_TIMESTAMP not null on update CURRENT_TIMESTAMP
);

create index idx_dag_node_graph_id
    on dag_graph_node (graph_id);

create index idx_dag_node_owner_id
    on dag_graph_node (owner_id);

-- ----------------------------
-- Table structure for dataset_info
-- ----------------------------
create table if not exists dataset_info
(
    id            bigint auto_increment comment 'PK' primary key,
    dataset_name  varchar(255)                               not null,
    project_id    bigint                                     null,
    owner_id      bigint                                     not null,
    is_deleted    tinyint unsigned default '0'               not null,
    created_time  datetime         default CURRENT_TIMESTAMP not null,
    modified_time datetime         default CURRENT_TIMESTAMP not null on update CURRENT_TIMESTAMP
);

create index idx_dataset_name
    on dataset_info (dataset_name);

create index idx_owner_id
    on dataset_info (owner_id);

create index idx_project_id
    on dataset_info (project_id);

-- ----------------------------
-- Table structure for dataset_version
-- ----------------------------
create table if not exists dataset_version
(
    id             bigint auto_increment comment 'PK' primary key,
    dataset_id     bigint                                     not null,
    owner_id       bigint                                     not null,
    version_name   varchar(255)                               not null,
    version_tag    varchar(255)                               null,
    version_meta   text                                       not null,
    files_uploaded text                                       null,
    storage_path   text                                       not null,
    status         tinyint unsigned default '0'               not null,
    created_time   datetime         default CURRENT_TIMESTAMP not null,
    modified_time  datetime         default CURRENT_TIMESTAMP not null on update CURRENT_TIMESTAMP,
    constraint unq_swds_version_name
        unique (dataset_id, version_name)
);

create index idx_dataset_id
    on dataset_version (dataset_id);

create index idx_owner_id
    on dataset_version (owner_id);

-- ----------------------------
-- Table structure for job_dataset_version_rel
-- ----------------------------
create table if not exists job_dataset_version_rel
(
    id                 bigint auto_increment comment 'PK' primary key,
    job_id             bigint                             not null,
    dataset_version_id bigint                             not null,
    created_time       datetime default CURRENT_TIMESTAMP not null,
    modified_time      datetime default CURRENT_TIMESTAMP not null on update CURRENT_TIMESTAMP
);

create index idx_dataset_version_id
    on job_dataset_version_rel (dataset_version_id);

create index idx_job_id
    on job_dataset_version_rel (job_id);

-- ----------------------------
-- Table structure for job_info
-- ----------------------------
create table if not exists job_info
(
    id                 bigint auto_increment comment 'PK' primary key,
    job_uuid           varchar(255)                 not null,
    project_id         bigint                       not null,
    swmp_version_id    bigint                       not null,
    owner_id           bigint                       not null,
    created_time       datetime                     not null,
    finished_time      datetime                     null,
    duration_ms        bigint                       not null,
    job_status         varchar(50)                  not null,
    job_type           varchar(50)                  not null,
    swrt_version_id    bigint                       not null,
    device_type        tinyint unsigned             not null,
    device_amount      int                          not null,
    result_output_path text                         not null,
    job_comment        text                         null,
    is_deleted         tinyint unsigned default '0' not null,
    constraint uk_job_uuid
        unique (job_uuid)
);

create index idx_base_image
    on job_info (swrt_version_id);

create index idx_job_status
    on job_info (job_status);

create index idx_owner_id
    on job_info (owner_id);

create index idx_swmp_version_id
    on job_info (swmp_version_id);

-- ----------------------------
-- Table structure for project_info
-- ----------------------------
create table if not exists project_info
(
    id            bigint auto_increment comment 'PK' primary key,
    project_name  varchar(255)                               not null,
    owner_id      bigint                                     not null,
    is_default    tinyint unsigned default '0'               not null,
    is_deleted    tinyint unsigned default '0'               not null,
    created_time  datetime         default CURRENT_TIMESTAMP not null,
    modified_time datetime         default CURRENT_TIMESTAMP not null on update CURRENT_TIMESTAMP
);

create index idx_create_user_id
    on project_info (owner_id);

create index idx_project_name
    on project_info (project_name);

-- ----------------------------
-- Table structure for runtime_info
-- ----------------------------
create table if not exists runtime_info
(
    id            bigint auto_increment comment 'PK' primary key,
    runtime_name  varchar(255)                               not null,
    project_id    bigint                                     not null,
    owner_id      bigint                                     not null,
    is_deleted    tinyint unsigned default '0'               not null,
    created_time  datetime         default CURRENT_TIMESTAMP not null,
    modified_time datetime         default CURRENT_TIMESTAMP not null on update CURRENT_TIMESTAMP
);

create index idx_owner_id
    on runtime_info (owner_id);

create index idx_project_id
    on runtime_info (project_id);

create index idx_runtime_name
    on runtime_info (runtime_name);

-- ----------------------------
-- Table structure for runtime_version
-- ----------------------------
create table if not exists runtime_version
(
    id            bigint auto_increment comment 'PK' primary key,
    runtime_id    bigint                             not null,
    owner_id      bigint                             not null,
    version_name  varchar(255)                       not null,
    version_tag   varchar(255)                       null,
    version_meta  text                               not null,
    storage_path  text                               not null,
    manifest      text                               null,
    created_time  datetime default CURRENT_TIMESTAMP not null,
    modified_time datetime default CURRENT_TIMESTAMP not null on update CURRENT_TIMESTAMP,
    constraint unq_runtime_version_name
        unique (runtime_id, version_name)
);

create index idx_owner_id
    on runtime_version (owner_id);

create index idx_runtime_id
    on runtime_version (runtime_id);

-- ----------------------------
-- Table structure for step
-- ----------------------------
create table if not exists step
(
    id            bigint auto_increment comment 'PK' primary key,
    step_uuid     varchar(255)                       not null,
    step_name     varchar(255)                       not null,
    job_id        bigint                             not null,
    last_step_id  bigint                             null,
    step_status   varchar(50)                        not null,
    finished_time datetime                           null,
    started_time  datetime                           null,
    created_time  datetime default CURRENT_TIMESTAMP not null,
    modified_time datetime default CURRENT_TIMESTAMP not null on update CURRENT_TIMESTAMP,
    constraint uk_step_uuid
        unique (step_uuid)
);

create index idx_step_job_id
    on step (job_id);

-- ----------------------------
-- Table structure for swmp_info
-- ----------------------------
create table if not exists swmp_info
(
    id            bigint auto_increment comment 'PK' primary key,
    swmp_name     varchar(255)                               not null,
    project_id    bigint                                     not null,
    owner_id      bigint                                     not null,
    is_deleted    tinyint unsigned default '0'               not null,
    created_time  datetime         default CURRENT_TIMESTAMP not null,
    modified_time datetime         default CURRENT_TIMESTAMP not null on update CURRENT_TIMESTAMP
);

create index idx_owner_id
    on swmp_info (owner_id);

create index idx_project_id
    on swmp_info (project_id);

create index idx_swmp_name
    on swmp_info (swmp_name);

-- ----------------------------
-- Table structure for swmp_version
-- ----------------------------
create table if not exists swmp_version
(
    id            bigint auto_increment comment 'PK' primary key,
    swmp_id       bigint                             not null,
    owner_id      bigint                             not null,
    version_name  varchar(255)                       not null,
    version_tag   varchar(255)                       null,
    version_meta  text                               not null,
    storage_path  text                               not null,
    manifest      text                               null,
    created_time  datetime default CURRENT_TIMESTAMP not null,
    modified_time datetime default CURRENT_TIMESTAMP not null on update CURRENT_TIMESTAMP,
    constraint unq_swmp_version_name
        unique (swmp_id, version_name)
);

create index idx_owner_id
    on swmp_version (owner_id);

create index idx_swmp_id
    on swmp_version (swmp_id);

-- ----------------------------
-- Table structure for task_info
-- ----------------------------
create table if not exists task_info
(
    id            bigint auto_increment comment 'PK' primary key,
    task_uuid     varchar(255)                       not null,
    step_id       bigint                             not null,
    agent_id      bigint                             null,
    task_status   varchar(50)                        not null,
    task_type     varchar(50)                        not null,
    result_path   text                               not null,
    task_request  longtext                           null,
    finished_time datetime                           null,
    started_time  datetime                           null,
    created_time  datetime default CURRENT_TIMESTAMP not null,
    modified_time datetime default CURRENT_TIMESTAMP not null on update CURRENT_TIMESTAMP,
    constraint uk_task_uuid
        unique (task_uuid)
);

create index idx_agent_id
    on task_info (agent_id);

create index idx_job_id
    on task_info (step_id);

create index idx_task_status
    on task_info (task_status);

create index idx_task_type
    on task_info (task_type);

-- ----------------------------
-- Table structure for user_info
-- ----------------------------
create table if not exists user_info
(
    id            bigint auto_increment comment 'PK' primary key,
    user_name     varchar(255)                               not null,
    user_pwd      varchar(255)                               not null,
    user_pwd_salt varchar(255)                               not null,
    role_id       bigint                                     not null,
    user_enabled  tinyint unsigned default '0'               not null,
    created_time  datetime         default CURRENT_TIMESTAMP not null,
    modified_time datetime         default CURRENT_TIMESTAMP not null on update CURRENT_TIMESTAMP,
    constraint uk_user_name
        unique (user_name)
);

-- ----------------------------
-- Table structure for user_role
-- ----------------------------
create table if not exists user_role
(
    id           bigint auto_increment comment 'PK' primary key,
    role_name    varchar(32) not null,
    role_name_en varchar(32) not null,
    constraint uk_role_name
        unique (role_name)
);



