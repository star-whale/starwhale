/*
 * Copyright 2022 Starwhale, Inc. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package ai.starwhale.mlops.api;

import ai.starwhale.mlops.api.protocol.Code;
import ai.starwhale.mlops.api.protocol.ResponseMessage;
import ai.starwhale.mlops.api.protocol.datastore.ColumnDesc;
import ai.starwhale.mlops.api.protocol.datastore.FlushRequest;
import ai.starwhale.mlops.api.protocol.datastore.ListTablesRequest;
import ai.starwhale.mlops.api.protocol.datastore.QueryTableRequest;
import ai.starwhale.mlops.api.protocol.datastore.RecordListVo;
import ai.starwhale.mlops.api.protocol.datastore.ScanTableRequest;
import ai.starwhale.mlops.api.protocol.datastore.TableNameListVo;
import ai.starwhale.mlops.api.protocol.datastore.TableQueryFilterDesc;
import ai.starwhale.mlops.api.protocol.datastore.TableQueryOperandDesc;
import ai.starwhale.mlops.api.protocol.datastore.UpdateTableRequest;
import ai.starwhale.mlops.datastore.ColumnSchema;
import ai.starwhale.mlops.datastore.ColumnType;
import ai.starwhale.mlops.datastore.DataStore;
import ai.starwhale.mlops.datastore.DataStoreQueryRequest;
import ai.starwhale.mlops.datastore.DataStoreScanRequest;
import ai.starwhale.mlops.datastore.RecordList;
import ai.starwhale.mlops.datastore.TableQueryFilter;
import ai.starwhale.mlops.datastore.exporter.RecordsExporter;
import ai.starwhale.mlops.datastore.impl.RecordDecoder;
import ai.starwhale.mlops.exception.SwProcessException;
import ai.starwhale.mlops.exception.SwProcessException.ErrorType;
import ai.starwhale.mlops.exception.SwValidationException;
import ai.starwhale.mlops.exception.SwValidationException.ValidSubject;
import java.io.IOException;
import java.text.MessageFormat;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import javax.annotation.Resource;
import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServletResponse;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("${sw.controller.api-prefix}")
@Slf4j
public class DataStoreController implements DataStoreApi {

    private static final Pattern COLUMN_NAME_PATTERN = Pattern.compile("^[\\p{Alnum}-_/: ]*$");

    @Resource
    @Setter
    private DataStore dataStore;

    @Resource
    @Setter
    private RecordsExporter recordsExporter;

    public ResponseEntity<ResponseMessage<TableNameListVo>> listTables(ListTablesRequest request) {
        return ResponseEntity.ok(
                Code.success.asResponse(new TableNameListVo(this.dataStore.list(request.getPrefix()))));
    }

    @Override
    public ResponseEntity<ResponseMessage<String>> updateTable(UpdateTableRequest request) {
        try {
            if (request.getTableName() == null) {
                throw new SwValidationException(SwValidationException.ValidSubject.DATASTORE,
                        "table name should not be null");
            }
            if (request.getTableSchemaDesc() == null) {
                throw new SwValidationException(SwValidationException.ValidSubject.DATASTORE,
                        "table schema should not be null");
            }
            List<Map<String, Object>> records;
            if (request.getRecords() == null) {
                records = List.of();
            } else {
                records = request.getRecords()
                        .stream()
                        .map(x -> {
                            if (x.getValues() == null) {
                                throw new SwValidationException(SwValidationException.ValidSubject.DATASTORE,
                                        "values should not be null. " + x);
                            }
                            var ret = new HashMap<String, Object>();
                            for (var r : x.getValues()) {
                                ret.put(r.getKey(), r.getValue());
                            }
                            return ret;
                        })
                        .collect(Collectors.toList());
            }
            var revision = this.dataStore.update(request.getTableName(), request.getTableSchemaDesc(), records);
            return ResponseEntity.ok(Code.success.asResponse(revision));
        } catch (SwValidationException e) {
            throw new SwValidationException(SwValidationException.ValidSubject.DATASTORE, "request=" + request, e);
        }
    }

    @Override
    public ResponseEntity<ResponseMessage<String>> flush(FlushRequest request) {
        this.dataStore.flush();
        return ResponseEntity.ok(Code.success.asResponse("success"));
    }

    @Override
    public ResponseEntity<ResponseMessage<RecordListVo>> queryTable(QueryTableRequest request) {
        try {
            RecordList recordList = queryRecordList(request);
            var vo = RecordListVo.builder().records(recordList.getRecords());
            if (!request.isEncodeWithType()) {
                vo.columnTypes(recordList.getColumnSchemaMap().values().stream()
                        .map(ColumnSchema::toColumnSchemaDesc)
                        .collect(Collectors.toList()));
            }
            return ResponseEntity.ok(Code.success.asResponse(vo.build()));
        } catch (SwValidationException e) {
            throw new SwValidationException(SwValidationException.ValidSubject.DATASTORE, "request=" + request, e);
        }
    }

    @Override
    public ResponseEntity<ResponseMessage<RecordListVo>> scanTable(ScanTableRequest request) {
        try {
            if (request.getTables() == null || request.getTables().isEmpty()) {
                throw new SwValidationException(SwValidationException.ValidSubject.DATASTORE,
                        "tables should not be null or empty.");
            }
            var recordList = this.dataStore.scan(DataStoreScanRequest.builder()
                    .tables(request.getTables().stream()
                            .map(x -> {
                                if (x == null) {
                                    throw new SwValidationException(SwValidationException.ValidSubject.DATASTORE,
                                            "table description should not be null");
                                }
                                if (x.getTableName() == null || x.getTableName().isEmpty()) {
                                    throw new SwValidationException(SwValidationException.ValidSubject.DATASTORE,
                                            "table name should not be null or empty: " + x);
                                }
                                return DataStoreScanRequest.TableInfo.builder()
                                        .tableName(x.getTableName())
                                        .columnPrefix(x.getColumnPrefix())
                                        .columns(DataStoreController.convertColumns(x.getColumns()))
                                        .keepNone(x.isKeepNone())
                                        .build();
                            })
                            .collect(Collectors.toList()))
                    .start(request.getStart())
                    .startType(request.getStartType())
                    .startInclusive(request.isStartInclusive())
                    .end(request.getEnd())
                    .endType(request.getEndType())
                    .endInclusive(request.isEndInclusive())
                    .limit(request.getLimit())
                    .keepNone(request.isKeepNone())
                    .rawResult(request.isRawResult())
                    .encodeWithType(request.isEncodeWithType())
                    .ignoreNonExistingTable(request.isIgnoreNonExistingTable())
                    .build());
            var vo = RecordListVo.builder()
                    .records(recordList.getRecords())
                    .lastKey(recordList.getLastKey());
            if (!request.isEncodeWithType()) {
                vo.columnTypes(recordList.getColumnSchemaMap().values().stream()
                        .map(ColumnSchema::toColumnSchemaDesc)
                        .collect(Collectors.toList()));
            }
            return ResponseEntity.ok(Code.success.asResponse(vo.build()));
        } catch (SwValidationException e) {
            throw new SwValidationException(SwValidationException.ValidSubject.DATASTORE, "request=" + request, e);
        }
    }

    @Override
    public void queryAndExport(QueryTableRequest request, HttpServletResponse httpResponse) {
        try {
            RecordList recordList = queryRecordList(request);
            byte[] bytes = recordsExporter.asBytes(recordList);
            httpResponse.addHeader("Content-Disposition",
                    "attachment; filename=\"" + request.getTableName() + ".csv\"");
            httpResponse.addHeader("Content-Length", String.valueOf(bytes.length));
            ServletOutputStream outputStream = httpResponse.getOutputStream();
            outputStream.write(bytes);
            outputStream.flush();
        } catch (SwValidationException e) {
            throw new SwValidationException(SwValidationException.ValidSubject.DATASTORE, "request=" + request, e);
        } catch (IOException e) {
            log.error("writing response failed", e);
            throw new SwProcessException(ErrorType.SYSTEM, "export records failed ", e);
        }
    }

    private RecordList queryRecordList(QueryTableRequest request) {
        if (request.getTableName() == null) {
            throw new SwValidationException(ValidSubject.DATASTORE,
                    "table name should not be null");
        }
        return this.dataStore.query(DataStoreQueryRequest.builder()
                .tableName(request.getTableName())
                .columns(DataStoreController.convertColumns(request.getColumns()))
                .filter(DataStoreController.convertFilter(request.getFilter()))
                .orderBy(request.getOrderBy())
                .start(request.getStart())
                .limit(request.getLimit())
                .keepNone(request.isKeepNone())
                .rawResult(request.isRawResult())
                .encodeWithType(request.isEncodeWithType())
                .ignoreNonExistingTable(request.isIgnoreNonExistingTable())
                .build());
    }

    private static TableQueryFilter convertFilter(TableQueryFilterDesc input) {
        if (input == null) {
            return null;
        }
        if (input.getOperator() == null) {
            throw new SwValidationException(SwValidationException.ValidSubject.DATASTORE,
                    "operator should not be empty. " + input);
        }
        if (input.getOperands() == null || input.getOperands().isEmpty()) {
            throw new SwValidationException(SwValidationException.ValidSubject.DATASTORE,
                    "operands should not be empty. " + input);
        }

        TableQueryFilter.Operator operator;
        try {
            operator = TableQueryFilter.Operator.valueOf(input.getOperator());
        } catch (IllegalArgumentException e) {
            throw new SwValidationException(SwValidationException.ValidSubject.DATASTORE,
                    "invalid operator " + input.getOperator() + ". " + input,
                    e);
        }
        switch (operator) {
            case NOT:
                if (input.getOperands().size() != 1) {
                    throw new SwValidationException(SwValidationException.ValidSubject.DATASTORE,
                            "'NOT' should have only one operand. " + input);
                }
                break;
            case AND:
            case OR:
                if (input.getOperands().size() < 2) {
                    throw new SwValidationException(SwValidationException.ValidSubject.DATASTORE,
                            "'AND'/'OR' should have 2 operands at least. " + input);
                }
                break;
            case EQUAL:
            case LESS:
            case LESS_EQUAL:
            case GREATER:
            case GREATER_EQUAL:
                if (input.getOperands().size() != 2) {
                    throw new SwValidationException(SwValidationException.ValidSubject.DATASTORE,
                            "operator '" + operator + "' should have 2 operands. " + input);
                }
                break;
            default:
                throw new SwValidationException(SwValidationException.ValidSubject.DATASTORE,
                        "unexpected operator " + operator);
        }
        var ret = TableQueryFilter.builder()
                .operator(operator)
                .operands(input.getOperands()
                        .stream()
                        .map(DataStoreController::convertOperand)
                        .collect(Collectors.toList()))
                .build();
        switch (operator) {
            case NOT:
            case AND:
            case OR:
                for (var operand : ret.getOperands()) {
                    if (!(operand instanceof TableQueryFilter)) {
                        throw new SwValidationException(SwValidationException.ValidSubject.DATASTORE,
                                MessageFormat.format("unsupported operand {0} for operator {1}", operand, operator));
                    }
                }
                break;
            case EQUAL:
            case LESS:
            case LESS_EQUAL:
            case GREATER:
            case GREATER_EQUAL:
                boolean hasColumn = false;
                for (var operand : ret.getOperands()) {
                    if (operand instanceof TableQueryFilter
                            || (operand == null && operator != TableQueryFilter.Operator.EQUAL)) {
                        throw new SwValidationException(SwValidationException.ValidSubject.DATASTORE,
                                MessageFormat.format("unsupported operand {0} for operator {1}", operand, operator));
                    }
                    if (operand instanceof TableQueryFilter.Column) {
                        hasColumn = true;
                    }
                }
                if (!hasColumn) {
                    throw new SwValidationException(SwValidationException.ValidSubject.DATASTORE,
                            "operator " + operator + " should have at least one column operand");
                }
                break;
            default:
                throw new SwValidationException(SwValidationException.ValidSubject.DATASTORE,
                        "unexpected operator " + operator);
        }
        return ret;
    }

    private static Object convertOperand(TableQueryOperandDesc operand) {
        if (operand == null) {
            throw new SwValidationException(SwValidationException.ValidSubject.DATASTORE, "operand should not be null");
        }
        if (operand.getFilter() != null) {
            return DataStoreController.convertFilter(operand.getFilter());
        }
        if (operand.getColumnName() != null) {
            return new TableQueryFilter.Column(operand.getColumnName());
        }
        if (operand.getBoolValue() != null) {
            return new TableQueryFilter.Constant(ColumnType.BOOL, operand.getBoolValue());
        }
        if (operand.getIntValue() != null) {
            return new TableQueryFilter.Constant(ColumnType.INT64, operand.getIntValue());
        }
        if (operand.getFloatValue() != null) {
            return new TableQueryFilter.Constant(ColumnType.FLOAT64, operand.getFloatValue());
        }
        if (operand.getStringValue() != null) {
            return new TableQueryFilter.Constant(ColumnType.STRING, operand.getStringValue());
        }
        if (operand.getBytesValue() != null) {
            return new TableQueryFilter.Constant(ColumnType.BYTES,
                    RecordDecoder.decodeScalar(ColumnType.BYTES, operand.getBytesValue()));
        }
        return new TableQueryFilter.Constant(null, null);
    }

    private static Map<String, String> convertColumns(List<ColumnDesc> columns) {
        Map<String, String> ret = null;
        if (columns != null) {
            ret = new HashMap<>();
            for (var col : columns) {
                if (col == null) {
                    throw new SwValidationException(ValidSubject.DATASTORE, "column should not be null");
                }
                var name = col.getColumnName();
                if (name == null) {
                    throw new SwValidationException(ValidSubject.DATASTORE, "column name should not be null. " + col);
                }
                var alias = col.getAlias();
                if (alias == null) {
                    alias = name;
                }
                ret.put(name, alias);
            }
        }
        return ret;
    }
}
