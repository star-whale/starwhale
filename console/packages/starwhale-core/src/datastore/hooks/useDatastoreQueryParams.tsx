import { IListQuerySchema } from '../../server/schemas/list'
import { QueryTableRequest, ScanTableRequest, TableQueryFilterDesc } from '../schemas/datastore'
import { OPERATOR, DataTypes } from '../constants'
import { DatastorePageT } from '../types'
import _ from 'lodash'
import TableQueryFilter from '../schemas/TableQueryFilter'

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

function ToTableQueryFilter(
    items?: {
        property: string
        value: string
        op: OPERATOR
    }[]
): TableQueryFilterDesc | undefined {
    if (!items) return undefined
    if (!Array.isArray(items)) return undefined

    const filters = items
        .map((item: any) => {
            return {
                ...item,
                type: DataTypes.STRING,
            }
        })
        .filter((item: any) => {
            if (!_.isNumber(item.value) && _.isEmpty(item.value)) return false
            return item.value && item.op && item.property
        })
        .map((item: any) => {
            return TableQueryFilter.fromUI({
                operator: item.op,
                columnName: item?.property,
                value: item.value,
                type: item?.type,
            })?.toJSON()
        })

    return TableQueryFilter.AND(filters)
}

export function getQuery({ options, tableName }: TableQueryParamsT) {
    const { pageNum = 1, pageSize = 10 } = options || {}
    const { start, limit, query } = {
        start: (pageNum - 1) * pageSize ?? 0,
        limit: pageSize ?? 0,
        query: options?.query,
    }
    const filter = ToTableQueryFilter(options?.filter)
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
        raw.filter = { ...filter }
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
