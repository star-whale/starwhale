export interface ColumnSchemaDesc {
    name?: string
    type?: string
    pythonType?: string
    elementType?: ColumnSchemaDesc
    keyType?: ColumnSchemaDesc
    valueType?: ColumnSchemaDesc
}

export interface RecordDesc {
    values: RecordValueDesc[]
}

export interface RecordValueDesc {
    key: string
    value?: object
}

export interface TableSchemaDesc {
    keyColumn?: string
    columnSchemaList?: ColumnSchemaDesc[]
}

export interface UpdateTableRequest {
    tableName?: string
    tableSchemaDesc?: TableSchemaDesc
    records?: RecordDesc[]
}

export interface ColumnDesc {
    columnName?: string
    alias?: string
}

export interface ScanTableRequest {
    tables?: TableDesc[]
    start?: string
    startType?: string
    startInclusive?: boolean
    end?: string
    endType?: string
    endInclusive?: boolean
    /** @format int32 */
    limit?: number
    keepNone?: boolean
    rawResult?: boolean
    encodeWithType?: boolean
    ignoreNonExistingTable?: boolean
}

export interface TableDesc {
    tableName?: string
    columnPrefix?: string
    columns?: ColumnDesc[]
    keepNone?: boolean
    revision?: string
}

export interface ColumnHintsDesc {
    typeHints?: string[]
    columnValueHints?: string[]
    elementHints?: ColumnHintsDesc
    keyHints?: ColumnHintsDesc
    valueHints?: ColumnHintsDesc
}

export interface RecordListVo {
    columnTypes?: ColumnSchemaDesc[]
    columnHints?: Record<string, ColumnHintsDesc>
    records?: Record<string, object>[]
    lastKey?: string
}

export interface ResponseMessageRecordListVo {
    code: string
    message: string
    data: RecordListVo
}

export interface OrderByDesc {
    columnName?: string
    descending?: boolean
}

export interface QueryTableRequest {
    tableName?: string
    columns?: ColumnDesc[]
    orderBy?: OrderByDesc[]
    descending?: boolean
    filter?: TableQueryFilterDesc
    /** @format int32 */
    start?: number
    /** @format int32 */
    limit?: number
    keepNone?: boolean
    rawResult?: boolean
    encodeWithType?: boolean
    ignoreNonExistingTable?: boolean
    revision?: string
}

export interface TableQueryFilterDesc {
    operator: string
    operands?: TableQueryOperandDesc[]
}

export interface TableQueryOperandDesc {
    filter?: TableQueryFilterDesc
    columnName?: string
    boolValue?: boolean
    /** @format int64 */
    intValue?: number
    /** @format double */
    floatValue?: number
    stringValue?: string
    bytesValue?: string
}

export interface ListTablesRequest {
    prefix?: string
    prefixes?: string[]
}

export interface ResponseMessageTableNameListVo {
    code?: string
    message?: string
    data?: TableNameListVo
}

export interface TableNameListVo {
    tables?: string[]
}
