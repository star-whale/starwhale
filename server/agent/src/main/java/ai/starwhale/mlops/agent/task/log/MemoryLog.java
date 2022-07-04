package ai.starwhale.mlops.agent.task.log;

import ai.starwhale.mlops.agent.task.inferencetask.InferenceTask;

import java.util.*;

public class MemoryLog implements Appender, Reader {
    /**
     * Long taskId
     * List<String> logCache
     */
    private final Map<Long, List<String>> logCache = new HashMap<>();

    /**
     * Long taskId
     * Map<readerId, offset>
     */
    private final Map<Long, Map<String, Integer>> offsets = new HashMap<>();

    @Override
    public void append(InferenceTask task, String content) {

    }

    @Override
    public void finishAppend(InferenceTask task) {

    }

    @Override
    public void read(String readerId, InferenceTask task) {

    }
}
