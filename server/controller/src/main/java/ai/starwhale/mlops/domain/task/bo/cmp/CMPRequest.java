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

    List<String> pplResultPaths;

    static final String LINE="\n";

    public CMPRequest(String txt){
        pplResultPaths = List.of(txt.split(LINE));
    }

    @Override
    public String toString() {
        return String.join(LINE, pplResultPaths);
    }

    @Override
    public TaskRequest deepCopy() {
        return new CMPRequest(List.copyOf(pplResultPaths));
    }


}
