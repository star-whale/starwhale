package ai.starwhale.mlops.agent.task.inferencetask;

import ai.starwhale.mlops.agent.container.ContainerClient;
import ai.starwhale.mlops.agent.task.inferencetask.persistence.TaskPersistence;
import ai.starwhale.mlops.api.protocol.report.req.TaskLog;
import ai.starwhale.mlops.api.protocol.report.resp.LogReader;
import com.github.dockerjava.api.async.ResultCallback;
import com.github.dockerjava.api.model.Frame;
import lombok.extern.slf4j.Slf4j;
import org.springframework.util.StringUtils;

import java.io.Closeable;
import java.io.IOException;
import java.util.*;

@Slf4j
public class LogRecorder {
    private final ContainerClient containerClient;
    private final TaskPersistence taskPersistence;
    /**
     * Long taskId
     * List<String> logCache
     */
    private final Map<Long, List<String>> logCache = new HashMap<>();

    /**
     * waiting to create container list
     */
    private final Set<Long> waiting = new HashSet<>();

    /**
     * Long taskId
     * Map<readerId, offset>
     */
    private final Map<Long, Map<String, Integer>> offsets = new HashMap<>();

    public LogRecorder(ContainerClient containerClient, TaskPersistence taskPersistence) {
        this.containerClient = containerClient;
        this.taskPersistence = taskPersistence;
    }

    public void addRecords(List<LogReader> logReaders) {
        for (LogReader logReader : logReaders) {
            if (!logCache.containsKey(logReader.getTaskId())) {
                List<String> cache = new ArrayList<>();
                logCache.put(logReader.getTaskId(), cache);
                offsets.put(logReader.getTaskId(), new HashMap<>() {{
                    put(logReader.getReaderId(), 0);
                }});
                waiting.add(logReader.getTaskId());
            } else {
                if (!offsets.get(logReader.getTaskId()).containsKey(logReader.getReaderId())) {
                    offsets.put(logReader.getTaskId(), new HashMap<>() {{
                        put(logReader.getReaderId(), 0);
                    }});
                }


            }
        }
        // todo clean unnecessary logCache and readerId
    }

    public List<TaskLog> generateLogs(Long taskId) {
        List<TaskLog> taskLogs = new ArrayList<>();
        List<String> logs = logCache.getOrDefault(taskId, new ArrayList<>());

        offsets.getOrDefault(taskId, Map.of()).forEach((readerId, offset) -> {
            ListIterator<String> iterator = logs.listIterator(offset);
            StringBuilder stringBuilder = new StringBuilder();
            while (iterator.hasNext()) {
                offset++;
                stringBuilder.append(iterator.next());
            }
            taskLogs.add(TaskLog.builder()
                    .readerId(readerId)
                    .log(stringBuilder.toString())
                    .build());
            offsets.get(taskId).put(readerId, offset);
        });
        return taskLogs;
    }

    /**
     * when container restart, must recall this method
     *
     * @param taskId      task id
     * @param containerId container
     */
    public void restart(Long taskId, String containerId) {
        logCache.put(taskId, new ArrayList<>());
        containerClient.logContainer(containerId,
                new LogRecord(taskId, containerId, logCache.get(taskId)));
    }

    // scheduled
    public void waitQueueScheduler() {
        for (Long taskId : new ArrayList<>(waiting)) {
            Optional<InferenceTask> taskOptional = taskPersistence.getActiveTaskById(taskId);
            if (taskOptional.isPresent()) {
                InferenceTask task = taskOptional.get();
                if (StringUtils.hasText(task.getContainerId())) {
                    // if container has been created
                    containerClient.logContainer(task.getContainerId(),
                            new LogRecord(taskId, task.getContainerId(), logCache.get(taskId)));
                    waiting.remove(taskId);
                }
            } else {
                // already archived
                log.warn("the task:{} seem to be archived, please try to get offline log!", taskId);
                remove(taskId);
            }
        }


    }

    /**
     * when task is succeed\failed\canceled,call this method
     *
     * @param taskId task id
     */
    public void remove(Long taskId) {
        waiting.remove(taskId);
        logCache.remove(taskId);
        offsets.remove(taskId);
    }

    public void removeReader(Long taskId, String readerId) {
        offsets.get(taskId).remove(readerId);
    }

    static class LogRecord implements ResultCallback<Frame> {
        Long taskId;
        String containerId;
        List<String> records;

        LogRecord(Long taskId, String containerId, List<String> records) {
            this.taskId = taskId;
            this.containerId = containerId;
            this.records = records;
        }

        @Override
        public void onStart(Closeable closeable) {
            log.info("start record for task:{}", taskId);
        }

        @Override
        public void onNext(Frame frame) {
            records.add(new String(frame.getPayload()));
        }

        @Override
        public void onError(Throwable throwable) {
            log.info("on error when record task:{}, error:{}", taskId, throwable.getMessage());
        }

        @Override
        public void onComplete() {
            log.info("complete record for task:{}", taskId);
        }

        @Override
        public void close() throws IOException {
            log.info("close record for task:{}", taskId);
        }
    }
}
