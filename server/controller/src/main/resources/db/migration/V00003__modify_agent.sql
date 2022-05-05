/*
 * Copyright 2022.1-2022
 * StarWhale.ai All right reserved. This software is the confidential and proprietary information of
 * StarWhale.ai ("Confidential Information"). You shall not disclose such Confidential Information and shall use it only
 * in accordance with the terms of the license agreement you entered into with StarWhale.ai.
 */
truncate TABLE agent_info;
ALTER TABLE `agent_info`
    ADD COLUMN `serial_number` VARCHAR(255) NOT NULL AFTER `agent_version`;
ALTER TABLE `agent_info` DROP INDEX `uk_node_ip`, ADD UNIQUE INDEX `uk_serial_number` (`serial_number`) USING BTREE;