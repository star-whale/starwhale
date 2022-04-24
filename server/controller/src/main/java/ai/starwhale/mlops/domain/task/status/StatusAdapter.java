/*
 * Copyright 2022.1-2022
 * StarWhale.ai All right reserved. This software is the confidential and proprietary information of
 * StarWhale.ai ("Confidential Information"). You shall not disclose such Confidential Information and shall use it only
 * in accordance with the terms of the license agreement you entered into with StarWhale.ai.
 */

package ai.starwhale.mlops.domain.task.status;

import ai.starwhale.mlops.api.protocol.TaskStatusInterface;
import java.util.AbstractMap.SimpleEntry;
import java.util.HashMap;
import java.util.Map;
import org.springframework.stereotype.Component;

/**
 * convert api status to bo status
 */
@Component
public class StatusAdapter {

    final static Map<TaskStatusInterface, TaskStatus> transferMapIn = Map.ofEntries(
        new SimpleEntry<>(TaskStatusInterface.CANCELED, TaskStatus.CANCELED)
        , new SimpleEntry<>(TaskStatusInterface.CANCELING, TaskStatus.CANCELLING)
        , new SimpleEntry<>(TaskStatusInterface.PREPARING, TaskStatus.PREPARING)
        , new SimpleEntry<>(TaskStatusInterface.RUNNING, TaskStatus.RUNNING)
        , new SimpleEntry<>(TaskStatusInterface.FAIL, TaskStatus.FAIL)
        , new SimpleEntry<>(TaskStatusInterface.SUCCESS, TaskStatus.SUCCESS));

    final Map<TaskStatus,TaskStatusInterface> transferMapOut;

    public StatusAdapter(){
        Map<TaskStatus,TaskStatusInterface> builder = new HashMap<>();
        transferMapIn.forEach((key,v)->{
            builder.put(v,key);
        });
        transferMapOut = Map.copyOf(builder);
    }

    public TaskStatus from(TaskStatusInterface tsi){
        return transferMapIn.get(tsi);
    }

    public TaskStatusInterface to(TaskStatus ts){
        return transferMapOut.get(ts);
    }

}
