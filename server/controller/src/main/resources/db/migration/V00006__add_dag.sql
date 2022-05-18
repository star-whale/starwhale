/*
 * Copyright 2022 Starwhale, Inc. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

CREATE TABLE IF NOT EXISTS dag_graph
(
    id            bigint           NOT NULL AUTO_INCREMENT COMMENT 'PK',
    job_id        bigint           NOT NULL,
    created_time  datetime         NOT NULL DEFAULT CURRENT_TIMESTAMP,
    modified_time datetime         NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    UNIQUE INDEX idx_dag_job_id (job_id) USING BTREE
    );

CREATE TABLE IF NOT EXISTS dag_graph_node
(
    id            bigint           NOT NULL AUTO_INCREMENT COMMENT 'PK',
    graph_id      bigint           NOT NULL,
    node_type     varchar(50)      NOT NULL,
    node_group    varchar(255)     NOT NULL,
    owner_id      bigint           NOT NULL,
    content       varchar(255)     NOT NULL,
    created_time  datetime         NOT NULL DEFAULT CURRENT_TIMESTAMP,
    modified_time datetime         NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    INDEX idx_dag_node_graph_id (graph_id) USING BTREE,
    INDEX idx_dag_node_owner_id (owner_id) USING BTREE
    );

CREATE TABLE IF NOT EXISTS dag_graph_edge
(
    id            bigint           NOT NULL AUTO_INCREMENT COMMENT 'PK',
    graph_id      bigint           NOT NULL,
    from_node     bigint           NOT NULL,
    to_node       bigint           NOT NULL,
    content       varchar(255)     NOT NULL,
    created_time  datetime         NOT NULL DEFAULT CURRENT_TIMESTAMP,
    modified_time datetime         NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    INDEX idx_dag_edge_graph_id (graph_id) USING BTREE,
    INDEX idx_dag_edge_from (from_node) USING BTREE,
    INDEX idx_dag_edge_to (to_node) USING BTREE
    );

