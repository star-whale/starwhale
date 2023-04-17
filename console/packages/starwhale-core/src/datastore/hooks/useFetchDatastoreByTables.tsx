import React from 'react'
import { useQueries } from 'react-query'
import { IListQuerySchema } from '../../server/schemas/list'
import { QueryTableRequest } from '../schemas/datastore'
import { RecordSchemaT } from '../types'
import { useScanDatastore } from './useFetchDatastore'
import { DataTypes } from '../constants'
import { TableScanParamsT, useDatastoreQueryParams, useDatastoreScanParams } from './useDatastoreQueryParams'
import { queryTable } from '../services/datastore'
import { SwType } from '../model'

export function useFetchDatastoreByTables(queries: TableScanParamsT) {
    const { columnQuery, recordQuery } = useDatastoreScanParams(queries)
    const columnInfo = useScanDatastore(columnQuery, true)
    const recordInfo = useScanDatastore(recordQuery, true)
    return {
        columnInfo,
        recordInfo,
        records: recordInfo.data?.records ?? [],
        columnTypes: columnInfo.data?.columnTypes ?? [],
    }
}

type TableQueryParamsT = {
    tableName?: string
    options?: IListQuerySchema & {
        filter?: any[]
        query?: QueryTableRequest
        revision?: string
    }
    enabled?: boolean
    prefix?: string
}

export function useFetchDatastoreByMergeTables(queries: TableQueryParamsT[]) {
    const { queries: $queries } = useDatastoreQueryParams(queries)

    const info = useQueries(
        $queries.map(({ recordQuery }, i: number) => {
            return {
                queryKey: ['queryDatastore', recordQuery.tableName],
                queryFn: () => queryTable(recordQuery),
                refetchOnWindowFocus: false,
            }
        })
    )

    const status = React.useMemo(() => {
        return info.map((i) => i.isSuccess).join(',')
    }, [info])

    const recordColumnCombineById = React.useMemo(() => {
        const result: Record<string, any> = {}
        let columns = new Set()
        let columnTypes: RecordSchemaT[] = []

        info.forEach(({ data }, i: number) => {
            if (!data) return
            const { records } = data
            Object.keys(records?.[0] ?? []).map((c) => columns.add(c))
            const prefix = queries[i].prefix
            records?.forEach((r: any) => {
                const id = r.id.value
                if (!result[id]) {
                    result[id] = {}
                }
                columns.forEach((c) => {
                    const schema = SwType.decode_schema(r[c])
                    result[id][`${prefix}${c}`] = schema
                })
            })
            Array.from(columns).map((c) => {
                columnTypes.push({
                    name: `${prefix}${c}`,
                    type: DataTypes.STRING,
                    mixed: true,
                    value: '',
                })
            })
        })
        return {
            records: Object.values(result),
            columnTypes,
        }
    }, [status])

    console.log(info, recordColumnCombineById)

    return {
        ...recordColumnCombineById,
    }
}

export default useFetchDatastoreByTables
