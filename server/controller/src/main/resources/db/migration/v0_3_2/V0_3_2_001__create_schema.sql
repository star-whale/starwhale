/*
 * Copyright 2022.1-2022
 * starwhale.ai All right reserved. This software is the confidential and proprietary information of
 * starwhale.ai ("Confidential Information"). You shall not disclose such Confidential Information and shall use it only
 * in accordance with the terms of the license agreement you entered into with starwhale.ai.
 */
-- ----------------------------
-- Table structure for agent_info
-- ----------------------------
CREATE TABLE IF NOT EXISTS `agent_info`
(
    `id`            bigint       NOT NULL AUTO_INCREMENT COMMENT 'PK',
    `agent_ip`      int UNSIGNED NOT NULL,
    `connect_time`  datetime     NOT NULL,
    `agent_version` varchar(255) NOT NULL,
    `agent_status`  varchar(32)  NOT NULL,
    `serial_number` varchar(255) NOT NULL,
    `device_info`   text         NOT NULL,
    `created_time`  datetime     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `modified_time` datetime     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`) USING BTREE,
    UNIQUE INDEX `uk_serial_number` (`serial_number` ASC) USING BTREE,
    INDEX `idx_agent_version` (`agent_version` ASC) USING BTREE
);

-- ----------------------------
-- Table structure for dag_graph
-- ----------------------------
CREATE TABLE IF NOT EXISTS `dag_graph`
(
    `id`            bigint   NOT NULL AUTO_INCREMENT COMMENT 'PK',
    `job_id`        bigint   NOT NULL,
    `created_time`  datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `modified_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`) USING BTREE,
    UNIQUE INDEX `idx_dag_job_id` (`job_id` ASC) USING BTREE
);

-- ----------------------------
-- Table structure for dag_graph_edge
-- ----------------------------
CREATE TABLE IF NOT EXISTS `dag_graph_edge`
(
    `id`            bigint       NOT NULL AUTO_INCREMENT COMMENT 'PK',
    `graph_id`      bigint       NOT NULL,
    `from_node`     bigint       NOT NULL,
    `to_node`       bigint       NOT NULL,
    `content`       varchar(255) NOT NULL,
    `created_time`  datetime     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `modified_time` datetime     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`) USING BTREE,
    INDEX `idx_dag_edge_from` (`from_node` ASC) USING BTREE,
    INDEX `idx_dag_edge_graph_id` (`graph_id` ASC) USING BTREE,
    INDEX `idx_dag_edge_to` (`to_node` ASC) USING BTREE
);

-- ----------------------------
-- Table structure for dag_graph_node
-- ----------------------------
CREATE TABLE IF NOT EXISTS `dag_graph_node`
(
    `id`            bigint       NOT NULL AUTO_INCREMENT COMMENT 'PK',
    `graph_id`      bigint       NOT NULL,
    `node_type`     varchar(50)  NOT NULL,
    `node_group`    varchar(255) NOT NULL,
    `owner_id`      bigint       NOT NULL,
    `content`       varchar(255) NOT NULL,
    `created_time`  datetime     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `modified_time` datetime     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`) USING BTREE,
    INDEX `idx_dag_node_graph_id` (`graph_id` ASC) USING BTREE,
    INDEX `idx_dag_node_owner_id` (`owner_id` ASC) USING BTREE
);

-- ----------------------------
-- Table structure for dataset_info
-- ----------------------------
CREATE TABLE IF NOT EXISTS `dataset_info`
(
    `id`            bigint           NOT NULL AUTO_INCREMENT COMMENT 'PK',
    `dataset_name`  varchar(255)     NOT NULL,
    `project_id`    bigint           NULL     DEFAULT NULL,
    `owner_id`      bigint           NOT NULL,
    `is_deleted`    tinyint UNSIGNED NOT NULL DEFAULT 0,
    `created_time`  datetime         NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `modified_time` datetime         NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`) USING BTREE,
    INDEX `idx_dataset_name` (`dataset_name` ASC) USING BTREE,
    INDEX `idx_owner_id` (`owner_id` ASC) USING BTREE,
    INDEX `idx_project_id` (`project_id` ASC) USING BTREE
);

-- ----------------------------
-- Table structure for dataset_read_log
-- ----------------------------
CREATE TABLE IF NOT EXISTS `dataset_read_log`
(
    `id`              bigint           NOT NULL AUTO_INCREMENT COMMENT 'PK',
    `session_id`      bigint           NOT NULL,
    `consumer_id`     varchar(255)     NULL     DEFAULT NULL,
    `start`           varchar(255)     NOT NULL,
    `start_inclusive` tinyint UNSIGNED NOT NULL DEFAULT 1,
    `end`             varchar(255)     NULL     DEFAULT NULL,
    `end_inclusive`   tinyint UNSIGNED NOT NULL DEFAULT 0,
    `status`          varchar(20)      NOT NULL,
    `size`            bigint           NOT NULL,
    `assigned_num`    bigint           NOT NULL DEFAULT 0,
    `assigned_time`   datetime         NULL     DEFAULT NULL,
    `finished_time`   datetime         NULL     DEFAULT NULL,
    `created_time`    datetime         NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `modified_time`   datetime         NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`) USING BTREE,
    UNIQUE INDEX `dataset_read_log_UK` (`session_id` ASC, `start` ASC, `end` ASC) USING BTREE
);

-- ----------------------------
-- Table structure for dataset_read_session
-- ----------------------------
CREATE TABLE IF NOT EXISTS `dataset_read_session`
(
    `id`              bigint           NOT NULL AUTO_INCREMENT COMMENT 'PK',
    `dataset_name`    varchar(255)     NOT NULL,
    `dataset_version` varchar(255)     NOT NULL,
    `table_name`      varchar(255)     NOT NULL,
    `start`           varchar(255)     NULL     DEFAULT NULL,
    `start_inclusive` tinyint UNSIGNED NOT NULL DEFAULT 1,
    `end`             varchar(255)     NULL     DEFAULT NULL,
    `end_inclusive`   tinyint UNSIGNED NOT NULL DEFAULT 1,
    `batch_size`      bigint           NOT NULL,
    `created_time`    datetime         NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `session_id`      varchar(255)     NOT NULL,
    PRIMARY KEY (`id`) USING BTREE,
    UNIQUE INDEX `dataset_read_session_UN` (`session_id` ASC, `dataset_name` ASC, `dataset_version`
                                            ASC) USING BTREE
);

-- ----------------------------
-- Table structure for dataset_version
-- ----------------------------
CREATE TABLE IF NOT EXISTS `dataset_version`
(
    `id`             bigint           NOT NULL AUTO_INCREMENT COMMENT 'PK',
    `version_order`  bigint           NOT NULL DEFAULT 0,
    `dataset_id`     bigint           NOT NULL,
    `owner_id`       bigint           NOT NULL,
    `version_name`   varchar(255)     NOT NULL,
    `version_tag`    varchar(255)     NULL     DEFAULT NULL,
    `version_meta`   mediumtext       NOT NULL,
    `files_uploaded` text             NULL,
    `storage_path`   text             NOT NULL,
    `status`         tinyint UNSIGNED NOT NULL DEFAULT 0,
    `created_time`   datetime         NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `modified_time`  datetime         NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    `size`           bigint           NOT NULL,
    `index_table`    varchar(255)     NOT NULL,
    PRIMARY KEY (`id`) USING BTREE,
    UNIQUE INDEX `unq_swds_version_name` (`dataset_id` ASC, `version_name` ASC) USING BTREE,
    INDEX `idx_dataset_id` (`dataset_id` ASC) USING BTREE,
    INDEX `idx_owner_id` (`owner_id` ASC) USING BTREE,
    INDEX `idx_version_order` (`version_order` ASC) USING BTREE
);

-- ----------------------------
-- Table structure for job_dataset_version_rel
-- ----------------------------
CREATE TABLE IF NOT EXISTS `job_dataset_version_rel`
(
    `id`                 bigint   NOT NULL AUTO_INCREMENT COMMENT 'PK',
    `job_id`             bigint   NOT NULL,
    `dataset_version_id` bigint   NOT NULL,
    `created_time`       datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `modified_time`      datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`) USING BTREE,
    INDEX `idx_dataset_version_id` (`dataset_version_id` ASC) USING BTREE,
    INDEX `idx_job_id` (`job_id` ASC) USING BTREE
);

-- ----------------------------
-- Table structure for job_info
-- ----------------------------
CREATE TABLE IF NOT EXISTS `job_info`
(
    `id`                 bigint           NOT NULL AUTO_INCREMENT COMMENT 'PK',
    `job_uuid`           varchar(255)     NOT NULL,
    `project_id`         bigint           NOT NULL,
    `model_version_id`   bigint           NOT NULL,
    `owner_id`           bigint           NOT NULL,
    `created_time`       datetime         NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `finished_time`      datetime         NULL     DEFAULT NULL,
    `duration_ms`        bigint           NULL     DEFAULT NULL,
    `job_status`         varchar(50)      NOT NULL,
    `job_type`           varchar(50)      NOT NULL,
    `runtime_version_id` bigint           NOT NULL,
    `result_output_path` text             NOT NULL,
    `step_spec`          text             NULL,
    `job_comment`        text             NULL,
    `is_deleted`         tinyint UNSIGNED NOT NULL DEFAULT 0,
    `resource_pool`      varchar(255)     NULL     DEFAULT NULL,
    PRIMARY KEY (`id`) USING BTREE,
    UNIQUE INDEX `uk_job_uuid` (`job_uuid` ASC) USING BTREE,
    INDEX `idx_base_image` (`runtime_version_id` ASC) USING BTREE,
    INDEX `idx_job_status` (`job_status` ASC) USING BTREE,
    INDEX `idx_owner_id` (`owner_id` ASC) USING BTREE,
    INDEX `idx_swmp_version_id` (`model_version_id` ASC) USING BTREE
);

-- ----------------------------
-- Table structure for model_info
-- ----------------------------
CREATE TABLE IF NOT EXISTS `model_info`
(
    `id`            bigint           NOT NULL AUTO_INCREMENT COMMENT 'PK',
    `model_name`    varchar(255)     NOT NULL,
    `project_id`    bigint           NOT NULL,
    `owner_id`      bigint           NOT NULL,
    `is_deleted`    tinyint UNSIGNED NOT NULL DEFAULT 0,
    `created_time`  datetime         NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `modified_time` datetime         NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`) USING BTREE,
    INDEX `idx_owner_id` (`owner_id` ASC) USING BTREE,
    INDEX `idx_project_id` (`project_id` ASC) USING BTREE,
    INDEX `idx_swmp_name` (`model_name` ASC) USING BTREE
);

-- ----------------------------
-- Table structure for model_version
-- ----------------------------
CREATE TABLE IF NOT EXISTS `model_version`
(
    `id`            bigint       NOT NULL AUTO_INCREMENT COMMENT 'PK',
    `version_order` bigint       NOT NULL DEFAULT 0,
    `model_id`      bigint       NOT NULL,
    `owner_id`      bigint       NOT NULL,
    `version_name`  varchar(255) NOT NULL,
    `version_tag`   varchar(255) NULL     DEFAULT NULL,
    `version_meta`  text         NOT NULL,
    `storage_path`  text         NOT NULL,
    `manifest`      text         NULL,
    `created_time`  datetime     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `modified_time` datetime     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    `eval_jobs`     text         NOT NULL,
    PRIMARY KEY (`id`) USING BTREE,
    UNIQUE INDEX `unq_swmp_version_name` (`model_id` ASC, `version_name` ASC) USING BTREE,
    INDEX `idx_owner_id` (`owner_id` ASC) USING BTREE,
    INDEX `idx_swmp_id` (`model_id` ASC) USING BTREE,
    INDEX `idx_version_order` (`version_order` ASC) USING BTREE
);

-- ----------------------------
-- Table structure for panel_plugin
-- ----------------------------
CREATE TABLE IF NOT EXISTS `panel_plugin`
(
    `id`            bigint       NOT NULL AUTO_INCREMENT COMMENT 'PK',
    `name`          varchar(255) NOT NULL,
    `version`       varchar(255) NOT NULL,
    `meta`          json         NULL,
    `storage_path`  text         NOT NULL,
    `created_time`  datetime     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `modified_time` datetime     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    `deleted_time`  datetime     NULL     DEFAULT NULL,
    PRIMARY KEY (`id`) USING BTREE,
    UNIQUE INDEX `uniq` (`name` ASC, `version` ASC, `deleted_time` ASC) USING BTREE
);

-- ----------------------------
-- Table structure for panel_setting
-- ----------------------------
CREATE TABLE IF NOT EXISTS `panel_setting`
(
    `user_id`       bigint       NOT NULL,
    `project_id`    bigint       NOT NULL,
    `name`          varchar(255) NOT NULL,
    `content`       text         NULL,
    `created_time`  datetime     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `modified_time` datetime     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (`name`, `project_id`) USING BTREE
);

-- ----------------------------
-- Table structure for project_info
-- ----------------------------
CREATE TABLE IF NOT EXISTS `project_info`
(
    `id`                  bigint           NOT NULL AUTO_INCREMENT COMMENT 'PK',
    `project_name`        varchar(255)     NOT NULL,
    `owner_id`            bigint           NOT NULL,
    `privacy`             int              NOT NULL DEFAULT 0,
    `project_description` text             NULL,
    `is_default`          tinyint UNSIGNED NOT NULL DEFAULT 0,
    `is_deleted`          tinyint UNSIGNED NOT NULL DEFAULT 0,
    `created_time`        datetime         NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `modified_time`       datetime         NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`) USING BTREE,
    UNIQUE INDEX `unq_project_name_owner` (`project_name` ASC, `owner_id` ASC) USING BTREE,
    INDEX `idx_create_user_id` (`owner_id` ASC) USING BTREE
);

-- ----------------------------
-- Table structure for runtime_info
-- ----------------------------
CREATE TABLE IF NOT EXISTS `runtime_info`
(
    `id`            bigint           NOT NULL AUTO_INCREMENT COMMENT 'PK',
    `runtime_name`  varchar(255)     NOT NULL,
    `project_id`    bigint           NOT NULL,
    `owner_id`      bigint           NOT NULL,
    `is_deleted`    tinyint UNSIGNED NOT NULL DEFAULT 0,
    `created_time`  datetime         NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `modified_time` datetime         NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`) USING BTREE,
    INDEX `idx_owner_id` (`owner_id` ASC) USING BTREE,
    INDEX `idx_project_id` (`project_id` ASC) USING BTREE,
    INDEX `idx_runtime_name` (`runtime_name` ASC) USING BTREE
);

-- ----------------------------
-- Table structure for runtime_version
-- ----------------------------
CREATE TABLE IF NOT EXISTS `runtime_version`
(
    `id`            bigint       NOT NULL AUTO_INCREMENT COMMENT 'PK',
    `version_order` bigint       NOT NULL DEFAULT 0,
    `runtime_id`    bigint       NOT NULL,
    `owner_id`      bigint       NOT NULL,
    `version_name`  varchar(255) NOT NULL,
    `version_tag`   varchar(255) NULL     DEFAULT NULL,
    `version_meta`  text         NOT NULL,
    `storage_path`  text         NOT NULL,
    `image`         text         NULL,
    `created_time`  datetime     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `modified_time` datetime     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`) USING BTREE,
    UNIQUE INDEX `unq_runtime_version_name` (`runtime_id` ASC, `version_name` ASC) USING BTREE,
    INDEX `idx_owner_id` (`owner_id` ASC) USING BTREE,
    INDEX `idx_runtime_id` (`runtime_id` ASC) USING BTREE,
    INDEX `idx_version_order` (`version_order` ASC) USING BTREE
);

-- ----------------------------
-- Table structure for step
-- ----------------------------
CREATE TABLE IF NOT EXISTS `step`
(
    `id`            bigint       NOT NULL AUTO_INCREMENT COMMENT 'PK',
    `step_uuid`     varchar(255) NOT NULL,
    `step_name`     varchar(255) NOT NULL,
    `job_id`        bigint       NOT NULL,
    `last_step_id`  bigint       NULL     DEFAULT NULL,
    `step_status`   varchar(50)  NOT NULL,
    `finished_time` datetime     NULL     DEFAULT NULL,
    `started_time`  datetime     NULL     DEFAULT NULL,
    `created_time`  datetime     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `modified_time` datetime     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    `concurrency`   int          NOT NULL,
    `task_num`      int          NOT NULL,
    PRIMARY KEY (`id`) USING BTREE,
    UNIQUE INDEX `uk_step_uuid` (`step_uuid` ASC) USING BTREE,
    INDEX `idx_step_job_id` (`job_id` ASC) USING BTREE
);

-- ----------------------------
-- Table structure for system_setting
-- ----------------------------
CREATE TABLE IF NOT EXISTS `system_setting`
(
    `id`      bigint NOT NULL AUTO_INCREMENT COMMENT 'PK',
    `content` text   NOT NULL COMMENT 'yaml format',
    PRIMARY KEY (`id`) USING BTREE
);

-- ----------------------------
-- Table structure for task_info
-- ----------------------------
CREATE TABLE IF NOT EXISTS `task_info`
(
    `id`            bigint       NOT NULL AUTO_INCREMENT COMMENT 'PK',
    `task_uuid`     varchar(255) NOT NULL,
    `step_id`       bigint       NOT NULL,
    `agent_id`      bigint       NULL     DEFAULT NULL,
    `task_status`   varchar(50)  NOT NULL,
    `task_request`  longtext     NULL,
    `finished_time` datetime     NULL     DEFAULT NULL,
    `started_time`  datetime     NULL     DEFAULT NULL,
    `created_time`  datetime     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `modified_time` datetime     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    `output_path`   text         NULL,
    PRIMARY KEY (`id`) USING BTREE,
    UNIQUE INDEX `uk_task_uuid` (`task_uuid` ASC) USING BTREE,
    INDEX `idx_agent_id` (`agent_id` ASC) USING BTREE,
    INDEX `idx_job_id` (`step_id` ASC) USING BTREE,
    INDEX `idx_task_status` (`task_status` ASC) USING BTREE
);

-- ----------------------------
-- Table structure for trash
-- ----------------------------
CREATE TABLE IF NOT EXISTS `trash`
(
    `id`            bigint       NOT NULL AUTO_INCREMENT COMMENT 'PK',
    `project_id`    bigint       NOT NULL,
    `object_id`     bigint       NOT NULL,
    `operator_id`   bigint       NOT NULL,
    `trash_name`    varchar(255) NOT NULL,
    `trash_type`    varchar(255) NOT NULL,
    `size`          bigint       NOT NULL DEFAULT 0,
    `retention`     datetime     NOT NULL,
    `updated_time`  datetime     NOT NULL,
    `created_time`  datetime     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `modified_time` datetime     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`) USING BTREE,
    INDEX `idx_project_id` (`project_id` ASC) USING BTREE,
    INDEX `idx_operator_id` (`operator_id` ASC) USING BTREE
);

-- ----------------------------
-- Table structure for user_info
-- ----------------------------
CREATE TABLE IF NOT EXISTS `user_info`
(
    `id`            bigint           NOT NULL AUTO_INCREMENT COMMENT 'PK',
    `user_name`     varchar(255)     NOT NULL,
    `user_pwd`      varchar(255)     NOT NULL,
    `user_pwd_salt` varchar(255)     NOT NULL,
    `user_enabled`  tinyint UNSIGNED NOT NULL DEFAULT 0,
    `created_time`  datetime         NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `modified_time` datetime         NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`) USING BTREE,
    UNIQUE INDEX `uk_user_name` (`user_name` ASC) USING BTREE
);

-- ----------------------------
-- Table structure for user_role_info
-- ----------------------------
CREATE TABLE IF NOT EXISTS `user_role_info`
(
    `id`               bigint       NOT NULL AUTO_INCREMENT COMMENT 'PK',
    `role_name`        varchar(32)  NOT NULL,
    `role_code`        varchar(32)  NOT NULL,
    `role_description` varchar(255) NOT NULL,
    PRIMARY KEY (`id`) USING BTREE,
    UNIQUE INDEX `uk_role_name` (`role_name` ASC) USING BTREE
);

-- ----------------------------
-- Table structure for user_role_rel
-- ----------------------------
CREATE TABLE IF NOT EXISTS `user_role_rel`
(
    `id`            bigint   NOT NULL AUTO_INCREMENT COMMENT 'PK',
    `user_id`       bigint   NOT NULL,
    `role_id`       bigint   NOT NULL,
    `project_id`    bigint   NOT NULL,
    `created_time`  datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `modified_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`) USING BTREE,
    UNIQUE INDEX `uk_project_role` (`user_id` ASC, `role_id` ASC, `project_id` ASC) USING BTREE,
    INDEX `idx_user_id` (`user_id` ASC) USING BTREE,
    INDEX `idx_role_id` (`role_id` ASC) USING BTREE,
    INDEX `idx_project_id` (`project_id` ASC) USING BTREE
);

-- ----------------------------
-- Table structure for view_config
-- ----------------------------
CREATE TABLE IF NOT EXISTS `view_config`
(
    `id`            bigint       NOT NULL AUTO_INCREMENT COMMENT 'PK',
    `config_name`   varchar(255) NOT NULL,
    `project_id`    bigint       NOT NULL,
    `owner_id`      bigint       NOT NULL,
    `content`       text         NOT NULL,
    `created_time`  datetime     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `modified_time` datetime     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`) USING BTREE,
    UNIQUE INDEX `uk_config_name` (`config_name` ASC, `owner_id` ASC, `project_id` ASC) USING BTREE
);
