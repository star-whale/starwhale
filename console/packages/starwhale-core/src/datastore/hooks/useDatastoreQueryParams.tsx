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

function getFilter(
    columnName: string,
    value: string,
    operator: OPERATOR,
    type: DataTypes
): TableQueryOperandDesc | undefined {
    let queryType = ''

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

    // operator that only render by frontend, like IN
    if (operator === OPERATOR.IN) {
        // eslint-disable-next-line no-param-reassign
        operator = OPERATOR.EQUAL
        // @ts-ignore
        // eslint-disable-next-line no-param-reassign
        value = Array.isArray(value) ? value : (value as string).split(',')
    }

    // single value
    if (!Array.isArray(value)) {
        operands.push({ [queryType]: value } as any)
        return {
            filter: {
                operator: operator as string,
                operands,
            },
        }
    }
    if (value.length === 0) return undefined
    // multiple value but only one, use single value
    if (value.length === 1) {
        operands.push({ [queryType]: value[0] } as any)
        return {
            filter: {
                operator: operator as string,
                operands,
            },
        }
    }
    // multiple value
    return {
        filter: {
            operator: 'OR',
            operands: value.map((v) => ({
                filter: {
                    operands: [{ columnName }, { [queryType]: v } as any],
                    operator: operator as string,
                },
            })),
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
        .filter((item: any) => {
            return item.value && item.op && item.property
        })
        .map((item: any) => {
            return getFilter(item?.property, item.value, item.op, item?.type as DataTypes)
        })

    if (!filters) return undefined

    if (filters.length === 1) return filters[0]?.filter

    if (filters.length === 0) return undefined

    return {
        operator: 'AND',
        operands: filters as any,
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
    const { pageSize = 10, lastKey } = options || {}
    const { limit } = {
        limit: pageSize ?? 0,
    }

    const recordQuery = {
        ...(options?.query ?? {}),
        tables,
        limit,
        rawResult: true,
        encodeWithType: true,
        ignoreNonExistingTable: true,
        ...(lastKey ? { start: lastKey } : {}),
    }

    return recordQuery as any
}
