/*
 * Copyright 2022.1-2022
 * StarWhale.ai All right reserved. This software is the confidential and proprietary information of
 * StarWhale.ai ("Confidential Information"). You shall not disclose such Confidential Information and shall use it only
 * in accordance with the terms of the license agreement you entered into with StarWhale.ai.
 */

CREATE TABLE IF NOT EXISTS user_info (
    id bigint NOT NULL AUTO_INCREMENT COMMENT 'PK',
    user_name varchar(255) NOT NULL,
    user_pwd varchar(255) NOT NULL,
    user_pwd_salt varchar(255) NOT NULL,
    role_id bigint NOT NULL,
    user_enabled tinyint UNSIGNED NOT NULL DEFAULT 0,
    created_time datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
    modified_time datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    UNIQUE INDEX uk_user_name(user_name) USING BTREE
);

CREATE TABLE IF NOT EXISTS user_role (
    id bigint NOT NULL AUTO_INCREMENT COMMENT 'PK',
    role_name varchar(32) NOT NULL,
    role_name_en varchar(32) NOT NULL,
    PRIMARY KEY (id),
    UNIQUE INDEX uk_role_name(role_name) USING BTREE
);

CREATE TABLE IF NOT EXISTS project_info (
    id bigint NOT NULL AUTO_INCREMENT COMMENT 'PK',
    project_name varchar(255) NOT NULL,
    owner_id bigint NOT NULL,
    is_deleted tinyint UNSIGNED NOT NULL DEFAULT 0,
    created_time datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
    modified_time datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    INDEX idx_project_name(project_name) USING BTREE,
    INDEX idx_create_user_id(owner_id) USING BTREE
);

CREATE TABLE IF NOT EXISTS agent_info (
    id bigint NOT NULL AUTO_INCREMENT COMMENT '主键，自增',
    agent_ip int UNSIGNED NOT NULL COMMENT '节点ip',
    connect_time datetime NOT NULL COMMENT '连接时间',
    agent_version varchar(255) NOT NULL COMMENT '节点版本',
    device_info varchar(255) NOT NULL COMMENT 'agent设备信息',
    created_time datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间，自更',
    modified_time datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '修改时间，自更',
    PRIMARY KEY (id),
    UNIQUE INDEX uk_node_ip(agent_ip) USING BTREE COMMENT '节点ip唯一约束',
    INDEX idx_agent_version(agent_version) USING BTREE
);

CREATE TABLE IF NOT EXISTS base_image (
    id bigint NOT NULL AUTO_INCREMENT COMMENT '主键，自增',
    image_name varchar(255) NOT NULL COMMENT '镜像名称',
    image_path varchar(255) NOT NULL COMMENT '镜像路径',
    created_time datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间，自更',
    modified_time datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '修改时间，自更',
    PRIMARY KEY (id),
    UNIQUE INDEX uk_image_name (image_name ) USING BTREE COMMENT '节点ip唯一约束'
);

CREATE TABLE IF NOT EXISTS swmp_info (
    id bigint NOT NULL AUTO_INCREMENT COMMENT '主键，自增',
    swmp_name varchar(255) NOT NULL COMMENT '模型名称',
    project_id bigint NOT NULL COMMENT '所属项目id',
    owner_id bigint NOT NULL COMMENT '创建者id',
    is_deleted tinyint UNSIGNED NOT NULL DEFAULT 0 COMMENT '删除标记',
    created_time datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间，自更',
    modified_time datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '修改时间，自更',
    PRIMARY KEY (id),
    INDEX idx_swmp_name(swmp_name) USING BTREE,
    INDEX idx_project_id(project_id) USING BTREE,
    INDEX idx_owner_id(owner_id) USING BTREE
);

CREATE TABLE IF NOT EXISTS swmp_version (
    id bigint NOT NULL AUTO_INCREMENT COMMENT '主键，自增',
    swmp_id bigint NOT NULL COMMENT '模型id',
    owner_id bigint NOT NULL COMMENT '创建者id',
    version_name varchar(255) NOT NULL COMMENT '模型版本',
    version_tag varchar(255) NOT NULL COMMENT '标签',
    version_meta TEXT NOT NULL COMMENT '描述信息',
    storage_path TEXT NOT NULL COMMENT '存储根路径',
    created_time datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间，自更',
    modified_time datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '修改时间，自更',
    PRIMARY KEY (id),
    INDEX idx_swmp_id(swmp_id) USING BTREE,
    INDEX idx_owner_id(owner_id) USING BTREE
);