export interface ColumnSchemaDesc {
    name: string
    type: string
    pythonType?: string
    elementType?: ColumnSchemaDesc
    attributes?: ColumnSchemaDesc[]
}

export interface RecordDesc {
    values: RecordValueDesc[]
}

export interface RecordValueDesc {
    key: string
    value?: string
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
    startInclusive?: boolean
    end?: string
    endInclusive?: boolean

    /** @format int32 */
    limit?: number
    keepNone?: boolean

    ignoreNonExistingTable?: boolean
}

export interface TableDesc {
    tableName?: string
    columns?: ColumnDesc[]
    keepNone?: boolean
    columnPrefix?: string
}

export interface RecordListVO {
    columnTypes?: ColumnSchemaDesc[]
    records?: Record<string, any>[]
    lastKey?: string
}

export interface ResponseMessageRecordListVO {
    code?: string
    message?: string
    data?: RecordListVO
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

    ignoreNonExistingTable?: boolean
    encodeWithType?: boolean
    keepNone?: boolean
    rawResult?: boolean
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
}
export interface TableNameListVO {
    tables?: string[]
}
