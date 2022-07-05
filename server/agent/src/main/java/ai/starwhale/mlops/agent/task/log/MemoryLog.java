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

package ai.starwhale.mlops.agent.task.log;

import ai.starwhale.mlops.agent.task.inferencetask.InferenceTask;
import ch.qos.logback.classic.PatternLayout;
import lombok.extern.slf4j.Slf4j;

import java.util.*;

@Slf4j
public class MemoryLog implements Appender, Reader {
    private final PatternLayout patternLayout;
    /**
     * Long taskId
     * List<String> logCache
     */
    private final Map<Long, LogObject> logCache = new HashMap<>();

    /**
     * Long taskId
     * Map<readerId, offset>
     */
    private final Map<Long, Map<String, Integer>> offsets = new HashMap<>();

    public MemoryLog(PatternLayout patternLayout) {
        this.patternLayout = patternLayout;
    }

    @Override
    public void append(InferenceTask task, LoggingEvent loggingEvent) {
        logCache.putIfAbsent(task.getId(), new LogObject());
        List<String> taskLog = logCache.get(task.getId()).getLogs();
        taskLog.add(patternLayout.doLayout(loggingEvent));
    }

    @Override
    public void finishAppend(InferenceTask task) {
        logCache.get(task.getId()).setFinished(true);
    }


    @Override
    public void subscribe(Long taskId, String readerId) {
        offsets.putIfAbsent(taskId, new HashMap<>());
        Map<String, Integer> taskOffset = offsets.get(taskId);
        taskOffset.putIfAbsent(readerId, 0);
    }

    @Override
    public void unSubscribe(Long taskId, String readerId) {
        if(offsets.containsKey(taskId)) {
            offsets.get(taskId).remove(readerId);
        }
        // clean when unsubscribe
        clean();
    }

    /**
     * timing or unsubscribe to do this action
     */
    public void clean() {
        for (Long taskId : logCache.keySet()) {
            // clean when no need for all readers
            if (offsets.get(taskId).isEmpty() && logCache.get(taskId).isFinished()) {
                offsets.remove(taskId);
                logCache.remove(taskId);
            }
        }
    }

    @Override
    public String read(Long taskId, String readerId) {
        if (!logCache.containsKey(taskId)) {
            log.info("No log has been generated for task:{}, please try to get it later", taskId);
            return null;
        }
        // try to subscribe to prevent no subscription
        subscribe(taskId, readerId);
        // start to read
        List<String> logs = logCache.get(taskId).getLogs();
        int lastIndex = offsets.get(taskId).get(readerId);
        ListIterator<String> iterator = logs.listIterator(lastIndex);
        StringBuilder stringBuilder = new StringBuilder();
        while (iterator.hasNext()) {
            lastIndex++;
            stringBuilder.append(iterator.next());
        }
        // update offset
        offsets.get(taskId).put(readerId, lastIndex);
        return stringBuilder.toString();
    }

    @Override
    public Map<String, String> read(Long taskId) {
        Map<String, String> results = new HashMap<>();
        Map<String, Integer> taskOffsets = offsets.get(taskId);
        taskOffsets.forEach((readerId, offset)->{
            results.put(readerId, read(taskId, readerId));
        });
        return results;
    }
}

class LogObject {
    private List<String> logs = new ArrayList<>();
    private boolean finished = false;

    public boolean isFinished() {
        return finished;
    }

    public void setFinished(boolean finished) {
        this.finished = finished;
    }

    public void append(String log) {
        logs.add(log);
    }

    public List<String> getLogs() {
        return logs;
    }
}
