import React, { useMemo } from 'react'
import { useQuery, useQueries } from 'react-query'
import qs from 'qs'
import { IListQuerySchema } from '../../server/schemas/list'
import { QueryTableRequest } from '../schemas/datastore'
import useDatastore from './useDatastore'
import { RecordSchemaT } from '../types'
import { useQueryDatastore } from './useFetchDatastore'
import { ColumnDesc, TableQueryFilterDesc, TableQueryOperandDesc } from '../schemas/datastore'
import { OPERATOR, DataTypes } from '../constants'
import { useDatastoreQueryParams } from './useDatastoreQueryParams'
import { queryTable } from '../services/datastore'
import { SwType } from '../model'

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

export function useFetchDatastoreByTables(queries: TableQueryParamsT[]) {
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
