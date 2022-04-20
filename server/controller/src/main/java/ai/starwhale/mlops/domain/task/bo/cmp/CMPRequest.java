package ai.starwhale.mlops.domain.task.bo.cmp;

import ai.starwhale.mlops.domain.task.bo.TaskRequest;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class CMPRequest extends TaskRequest {

    List<String> evaluationTaskPaths;

    static final String LINE="\n";

    public CMPRequest(String txt){
        evaluationTaskPaths = List.of(txt.split(LINE));
    }

    @Override
    public String toString() {
        return String.join(LINE,evaluationTaskPaths);
    }

    @Override
    public TaskRequest deepCopy() {
        return new CMPRequest(List.copyOf(evaluationTaskPaths));
    }


}
