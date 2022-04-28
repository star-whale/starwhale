package ai.starwhale.mlops.api.protocol.report.resp;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class LogReader {

    private Long taskId;

    private String readerId;
}
