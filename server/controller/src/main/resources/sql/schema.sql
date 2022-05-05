/*
 * Copyright 2022.1-2022
 * StarWhale.ai All right reserved. This software is the confidential and proprietary information of
 * StarWhale.ai ("Confidential Information"). You shall not disclose such Confidential Information and shall use it only
 * in accordance with the terms of the license agreement you entered into with StarWhale.ai.
 */
CREATE DATABASE IF NOT EXISTS starwhale;
use starwhale;
CREATE TABLE IF NOT EXISTS user_info
(
    id            bigint           NOT NULL AUTO_INCREMENT COMMENT 'PK',
    user_name     varchar(255)     NOT NULL,
    user_pwd      varchar(255)     NOT NULL,
    user_pwd_salt varchar(255)     NOT NULL,
    role_id       bigint           NOT NULL,
    user_enabled  tinyint UNSIGNED NOT NULL DEFAULT 0,
    created_time  datetime         NOT NULL DEFAULT CURRENT_TIMESTAMP,
    modified_time datetime         NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    UNIQUE INDEX uk_user_name (user_name) USING BTREE
);

CREATE TABLE IF NOT EXISTS user_role
(
    id           bigint      NOT NULL AUTO_INCREMENT COMMENT 'PK',
    role_name    varchar(32) NOT NULL,
    role_name_en varchar(32) NOT NULL,
    PRIMARY KEY (id),
    UNIQUE INDEX uk_role_name (role_name) USING BTREE
);

CREATE TABLE IF NOT EXISTS project_info
(
    id            bigint           NOT NULL AUTO_INCREMENT COMMENT 'PK',
    project_name  varchar(255)     NOT NULL,
    owner_id      bigint           NOT NULL,
    is_default    tinyint UNSIGNED NOT NULL DEFAULT 0,
    is_deleted    tinyint UNSIGNED NOT NULL DEFAULT 0,
    created_time  datetime         NOT NULL DEFAULT CURRENT_TIMESTAMP,
    modified_time datetime         NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    INDEX idx_project_name (project_name) USING BTREE,
    INDEX idx_create_user_id (owner_id) USING BTREE
);

CREATE TABLE IF NOT EXISTS agent_info
(
    id            bigint       NOT NULL AUTO_INCREMENT COMMENT 'PK',
    agent_ip      int UNSIGNED NOT NULL,
    serial_number varchar(255) NOT NULL,
    connect_time  datetime     NOT NULL,
    agent_version varchar(255) NOT NULL,
    device_info   varchar(255) NOT NULL,
    created_time  datetime     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    modified_time datetime     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    UNIQUE INDEX uk_node_ip (agent_ip) USING BTREE,
    INDEX idx_agent_version (agent_version) USING BTREE
);

CREATE TABLE IF NOT EXISTS base_image
(
    id            bigint       NOT NULL AUTO_INCREMENT COMMENT 'PK',
    image_name    varchar(255) NOT NULL,
    created_time  datetime     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    modified_time datetime     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    UNIQUE INDEX uk_image_name (image_name) USING BTREE
);

CREATE TABLE IF NOT EXISTS swmp_info
(
    id            bigint           NOT NULL AUTO_INCREMENT COMMENT 'PK',
    swmp_name     varchar(255)     NOT NULL,
    project_id    bigint           NOT NULL,
    owner_id      bigint           NOT NULL,
    is_deleted    tinyint UNSIGNED NOT NULL DEFAULT 0,
    created_time  datetime         NOT NULL DEFAULT CURRENT_TIMESTAMP,
    modified_time datetime         NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    INDEX idx_swmp_name (swmp_name) USING BTREE,
    INDEX idx_project_id (project_id) USING BTREE,
    INDEX idx_owner_id (owner_id) USING BTREE
);

CREATE TABLE IF NOT EXISTS swmp_version
(
    id            bigint       NOT NULL AUTO_INCREMENT COMMENT 'PK',
    swmp_id       bigint       NOT NULL,
    owner_id      bigint       NOT NULL,
    version_name  varchar(255) NOT NULL,
    version_tag   varchar(255) ,
    version_meta  TEXT         NOT NULL,
    storage_path  TEXT         NOT NULL,
    created_time  datetime     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    modified_time datetime     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    INDEX idx_swmp_id (swmp_id) USING BTREE,
    INDEX idx_owner_id (owner_id) USING BTREE,
    unique unq_swmp_version_name (swmp_id,version_name) USING BTREE
);

CREATE TABLE IF NOT EXISTS dataset_info
(
    id            bigint           NOT NULL AUTO_INCREMENT COMMENT 'PK',
    dataset_name  varchar(255)     NOT NULL,
    project_id    bigint           NULL,
    owner_id      bigint           NOT NULL,
    is_deleted    tinyint UNSIGNED NOT NULL DEFAULT 0,
    created_time  datetime         NOT NULL DEFAULT CURRENT_TIMESTAMP,
    modified_time datetime         NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    INDEX idx_dataset_name (dataset_name) USING BTREE,
    INDEX idx_project_id (project_id) USING BTREE,
    INDEX idx_owner_id (owner_id) USING BTREE
);

CREATE TABLE IF NOT EXISTS dataset_version
(
    id            bigint       NOT NULL AUTO_INCREMENT COMMENT 'PK',
    dataset_id    bigint       NOT NULL,
    owner_id      bigint       NOT NULL,
    version_name  varchar(255) NOT NULL,
    version_tag   varchar(255) ,
    version_meta  TEXT         NOT NULL,
    files_uploaded  TEXT       ,
    storage_path  TEXT         NOT NULL,
    status    tinyint UNSIGNED NOT NULL DEFAULT 0,
    created_time  datetime     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    modified_time datetime     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    INDEX idx_dataset_id (dataset_id) USING BTREE,
    INDEX idx_owner_id (owner_id) USING BTREE,
    unique unq_swds_version_name (dataset_id,version_name) USING BTREE
);

CREATE TABLE IF NOT EXISTS job_info
(
    id              bigint           NOT NULL AUTO_INCREMENT COMMENT 'PK',
    job_uuid        varchar(255)     NOT NULL,
    project_id      bigint           NOT NULL,
    swmp_version_id bigint           NOT NULL,
    owner_id        bigint           NOT NULL,
    created_time    datetime         NOT NULL,
    finished_time   datetime,
    duration_ms     bigint           NOT NULL,
    job_status      varchar(50)      NOT NULL,
    base_image_id   bigint           NOT NULL,
    device_type     tinyint UNSIGNED NOT NULL,
    device_amount   int              NOT NULL,
    result_output_path text          NOT NULL,
    PRIMARY KEY (id),
    UNIQUE INDEX uk_job_uuid (job_uuid) USING BTREE,
    INDEX idx_swmp_version_id (swmp_version_id) USING BTREE,
    INDEX idx_owner_id (owner_id) USING BTREE,
    INDEX idx_base_image (base_image_id) USING BTREE,
    INDEX idx_job_status (job_status) USING BTREE
);

CREATE TABLE IF NOT EXISTS job_dataset_version_rel
(
    id                 bigint   NOT NULL AUTO_INCREMENT COMMENT 'PK',
    job_id            bigint   NOT NULL,
    dataset_version_id bigint   NOT NULL,
    created_time       datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
    modified_time      datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    INDEX idx_job_id (job_id) USING BTREE,
    INDEX idx_dataset_version_id (dataset_version_id) USING BTREE
);

CREATE TABLE IF NOT EXISTS task_info
(
    id              bigint           NOT NULL AUTO_INCREMENT COMMENT 'PK',
    task_uuid       varchar(255)     NOT NULL,
    job_id          bigint           NOT NULL,
    agent_id        bigint           ,
    task_status     varchar(50)      NOT NULL,
    task_type       varchar(50)      NOT NULL,
    result_path     text             NOT NULL,
    task_request    longtext,
    created_time    datetime         NOT NULL DEFAULT CURRENT_TIMESTAMP,
    modified_time   datetime         NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    UNIQUE INDEX uk_task_uuid (task_uuid) USING BTREE,
    INDEX idx_job_id (job_id) USING BTREE,
    INDEX idx_agent_id (agent_id) USING BTREE,
    INDEX idx_task_status (task_status) USING BTREE,
    INDEX idx_task_type (task_type) USING BTREE
);
