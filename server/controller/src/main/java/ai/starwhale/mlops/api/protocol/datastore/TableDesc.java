package ai.starwhale.mlops.api.protocol.datastore;

import lombok.Data;

import java.util.List;

@Data
public class TableDesc {
    private String tableName;
    private List<ColumnDesc> columns;
    private boolean keepNone;
}
