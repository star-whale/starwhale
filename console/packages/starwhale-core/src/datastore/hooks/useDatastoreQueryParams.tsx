import { IListQuerySchema } from '../../server/schemas/list'
import { QueryTableRequest, ScanTableRequest, TableQueryFilterDesc, TableQueryOperandDesc } from '../schemas/datastore'
import { OPERATOR, DataTypes } from '../constants'
import { DatastorePageT } from '../types'

export type TableQueryParamsT = {
    tableName?: string
    options?: IListQuerySchema & {
        filter?: any[]
        query?: QueryTableRequest
        revision?: string
    }
    enabled?: boolean
}

export type TableScanParamsT = ScanTableRequest

function getFilter(columnName: string, value: string, operator: OPERATOR, type: DataTypes): TableQueryOperandDesc {
    let queryType
    switch (type) {
        case DataTypes.FLOAT16:
        case DataTypes.FLOAT32:
        case DataTypes.FLOAT64:
            queryType = 'floatValue'
            break
        case DataTypes.INT64:
            queryType = 'intValue'
            break
        case DataTypes.STRING:
            queryType = 'stringValue'
            break
        case DataTypes.BOOL:
            queryType = 'boolValue'
            break
        case DataTypes.BYTES:
            queryType = 'bytesValue'
            break
        default:
            queryType = 'stringValue'
            break
    }
    const operands = [{ columnName }]
    operands.push({ [queryType]: value } as any)
    return {
        filter: {
            operator: operator as string,
            operands,
        },
    }
}

function FilterToQuery(
    items: {
        property: string
        value: string
        op: OPERATOR
    }[],
    mixed = true
): TableQueryFilterDesc | undefined {
    const filters = items
        .map((item: any) => {
            if (mixed) {
                return {
                    ...item,
                    type: DataTypes.STRING,
                }
            }
            return item
        })
        .filter((item: any) => item.value && item.op && item.property)
        .map((item: any) => {
            return getFilter(item?.property, item.value, item.op, item?.type as DataTypes)
        })

    if (!filters) return undefined

    if (filters.length === 1) return filters[0]?.filter

    if (filters.length === 0) return undefined

    return {
        operator: 'AND',
        operands: filters,
    }
}

export function getQuery({ options, tableName }: TableQueryParamsT) {
    const { pageNum = 1, pageSize = 10 } = options || {}
    const { start, limit, query } = {
        start: (pageNum - 1) * pageSize ?? 0,
        limit: pageSize ?? 0,
        query: options?.query,
    }
    const filter = options?.filter && options?.filter.length > 0 ? FilterToQuery(options?.filter, true) : undefined
    const revision = options?.revision
    const raw: any = {
        ...(query ?? {}),
        tableName,
        start,
        limit,
        rawResult: true,
        ignoreNonExistingTable: true,
        encodeWithType: true,
    }
    if (revision) {
        raw.revision = revision
    }
    if (filter) {
        raw.filter = filter
    }
    return raw
}

export function getScanQuery(tables: ScanTableRequest['tables'], options: DatastorePageT): ScanTableRequest {
    const { pageNum = 1, pageSize = 10 } = options || {}
    const { start, limit } = {
        start: (pageNum - 1) * pageSize ?? 0,
        limit: pageSize ?? 0,
    }

    const recordQuery = {
        tables,
        start,
        limit,
        rawResult: true,
        encodeWithType: true,
        ignoreNonExistingTable: true,
    }

    return recordQuery as any
}
