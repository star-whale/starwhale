import React, { useMemo } from 'react'
import { useQuery } from 'react-query'
import qs from 'qs'
import { IListQuerySchema } from '../../server/schemas/list'
import { scanTable, queryTable, listTables, exportTable } from '../services/datastore'
import { QueryTableRequest } from '../schemas/datastore'
import { ColumnFilterModel } from '../filter'

export function useScanDatastore(query: any, enabled = false) {
    const info = useQuery(`scanDatastore:${qs.stringify(query)}`, () => scanTable(query), {
        refetchOnWindowFocus: false,
        enabled,
    })
    return info
}

export function useQueryDatastore(query?: QueryTableRequest, enable = true) {
    const info = useQuery(`queryDatastore:${qs.stringify(query)}`, () => queryTable(query as QueryTableRequest), {
        refetchOnWindowFocus: false,
        enabled: !!query?.tableName && enable,
    })
    return info
}

export function useListDatastoreTables(query: any, enabled = false) {
    const info = useQuery(`listTables:${qs.stringify(query)}`, () => listTables(query), {
        refetchOnWindowFocus: false,
        enabled,
    })
    return info
}

export function useExportDatastore(query?: QueryTableRequest, enable = true) {
    const info = useQuery(`exportDatastore:${qs.stringify(query)}`, () => exportTable(query as QueryTableRequest), {
        refetchOnWindowFocus: false,
        enabled: !!query?.tableName && enable,
    })
    return info
}

export function useQueryDatasetList(
    tableName?: string,
    options?: IListQuerySchema & {
        filter?: any
    },
    rawResult = false
) {
    const { start, limit } = React.useMemo(() => {
        const { pageNum = 1, pageSize = 10 } = options || {}

        return {
            start: (pageNum - 1) * pageSize ?? 0,
            limit: pageSize ?? 0,
        }
    }, [options])

    const columnInfo = useQueryDatastore({
        tableName,
        start: 0,
        limit: 0,
        rawResult,
        ignoreNonExistingTable: true,
    })

    const recordQuery = useMemo(() => {
        const column = new ColumnFilterModel(columnInfo.data?.columnTypes ?? [])
        const filter = options?.filter && options?.filter.length > 0 ? column.toQuery(options?.filter) : undefined
        const raw = {
            tableName,
            start,
            limit,
            rawResult,
            ignoreNonExistingTable: true,
        }
        return filter ? { ...raw, filter } : raw
    }, [options?.filter, columnInfo.data?.columnTypes, limit, rawResult, start, tableName])

    const recordInfo = useQueryDatastore(recordQuery)

    React.useEffect(() => {
        if (tableName) {
            columnInfo.refetch()
        }
        // eslint-disable-next-line react-hooks/exhaustive-deps
    }, [tableName, start, limit])

    React.useEffect(() => {
        if (recordQuery.tableName) {
            recordInfo.refetch()
        }
        // eslint-disable-next-line react-hooks/exhaustive-deps
    }, [recordQuery.tableName])

    return {
        columnInfo,
        recordInfo,
    }
}
