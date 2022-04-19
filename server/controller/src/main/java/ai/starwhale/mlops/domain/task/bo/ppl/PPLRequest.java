package ai.starwhale.mlops.domain.task.bo.ppl;

import ai.starwhale.mlops.domain.swds.index.SWDSBlock;
import ai.starwhale.mlops.domain.task.bo.TaskRequest;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class PPLRequest extends TaskRequest {

    /**
     * blocks may come from different SWDS
     */
    private List<SWDSBlock> swdsBlocks;

    @Override
    public TaskRequest deepCopy() {
        return new PPLRequest(List.copyOf(swdsBlocks));
    }

}
