package ai.starwhale.mlops.api.protocol.report.req;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class TaskLog {

    private String readerId;

    private String log;

}
