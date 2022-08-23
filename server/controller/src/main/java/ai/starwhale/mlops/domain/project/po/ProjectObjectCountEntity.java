package ai.starwhale.mlops.domain.project.po;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class ProjectObjectCountEntity {

    private Long projectId;

    private Integer countModel;

    private Integer countDataset;

    private Integer countMember;

    private Integer countJobs;
}
