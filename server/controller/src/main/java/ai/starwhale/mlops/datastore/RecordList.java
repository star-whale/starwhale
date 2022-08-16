package ai.starwhale.mlops.datastore;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.List;
import java.util.Map;

@Getter
@AllArgsConstructor
public class RecordList {
    private Map<String, ColumnType> columnTypeMap;
    private List<Map<String, String>> records;
    private String lastKey;
}
