package ai.starwhale.mlops.api.protocol.datastore;

import lombok.Data;

@Data
public class TableQueryOperandDesc {
    private TableQueryFilterDesc filter;
    private String columnName;
    private Boolean boolValue;
    private Long intValue;
    private Double floatValue;
    private String stringValue;
    private String bytesValue;
}
