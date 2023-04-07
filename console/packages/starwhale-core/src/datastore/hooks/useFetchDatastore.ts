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
        filter?: any[]
        query?: QueryTableRequest
        revision?: string
    },
    enabled = true
) {
    const { start, limit, query } = React.useMemo(() => {
        const { pageNum = 1, pageSize = 10 } = options || {}

        return {
            start: (pageNum - 1) * pageSize ?? 0,
            limit: pageSize ?? 0,
            query: options?.query ?? {},
        }
    }, [options])

    const columnInfo = useQueryDatastore(
        {
            tableName,
            start: 0,
            limit: 1,
            rawResult: true,
            ignoreNonExistingTable: true,
            encodeWithType: true,
            keepNone: true,
        },
        enabled
    )

    const recordQuery = useMemo(() => {
        const column = new ColumnFilterModel(columnInfo.data?.columnTypes ?? [])
        const filter = options?.filter && options?.filter.length > 0 ? column.toQuery(options?.filter) : undefined
        const revision = options?.revision
        const raw: any = {
            ...query,
            tableName,
            start,
            limit,
            rawResult: true,
            ignoreNonExistingTable: true,
            encodeWithType: true,
            keepNone: true,
        }
        if (revision) {
            raw.revision = revision
        }
        if (filter) {
            raw.filter = filter
        }
        return raw
    }, [options?.filter, columnInfo.data?.columnTypes, limit, start, tableName, query, options?.revision])

    const recordInfo = useQueryDatastore(recordQuery, columnInfo.isSuccess)

    React.useEffect(() => {
        if (tableName && enabled && columnInfo.isError) {
            columnInfo.refetch()
        }
        // eslint-disable-next-line react-hooks/exhaustive-deps
    }, [tableName, enabled])

    React.useEffect(() => {
        if (recordQuery.tableName && columnInfo.isSuccess) {
            recordInfo.refetch()
        }
        // eslint-disable-next-line react-hooks/exhaustive-deps
    }, [recordQuery.tableName])

    return {
        recordQuery,
        columnInfo,
        recordInfo,
        columnTypes: columnInfo.data?.columnTypes,
        records: recordInfo.data?.records,
    }
}
