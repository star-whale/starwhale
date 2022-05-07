package ai.starwhale.mlops.agent.task.inferencetask.action.normal.cancel;

import ai.starwhale.mlops.agent.task.inferencetask.InferenceStage;

import java.util.Optional;

public interface ExecuteStage {
    /**
     * represent current stage
     * @return current stage
     */
    default Optional<InferenceStage> stage() {
        return Optional.empty();
    }
}
