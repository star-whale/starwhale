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