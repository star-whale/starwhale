import React, { useMemo } from 'react'
import { useQuery } from 'react-query'
import qs from 'qs'
import { IListQuerySchema } from '../../server/schemas/list'
import { scanTable, queryTable, listTables, exportTable } from '../services/datastore'
import { QueryTableRequest } from '../schemas/datastore'
import { useDatastoreFilter } from '../filter'
import useDatastore from './useDatastore'
import { RecordSchemaT } from '../types'

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
            query: options?.query,
        }
    }, [options])

    const { toQuery } = useDatastoreFilter()

    const [columnQuery, recordQuery] = useMemo(() => {
        const filter = options?.filter && options?.filter.length > 0 ? toQuery(options?.filter) : undefined
        const revision = options?.revision
        const raw: any = {
            ...(query ?? {}),
            tableName,
            start,
            limit,
            rawResult: true,
            ignoreNonExistingTable: false,
            encodeWithType: true,
        }
        if (revision) {
            raw.revision = revision
        }
        if (filter) {
            raw.filter = filter
        }
        return [
            {
                ...raw,
                encodeWithType: false,
            },
            raw,
        ]
    }, [options?.filter, limit, start, tableName, query, options?.revision, toQuery])

    const columnInfo = useQueryDatastore(columnQuery, enabled)

    // useIfChanged({ filter: options?.filter, limit, start, tableName, query, revision: options?.revision, toQuery })

    const recordInfo = useQueryDatastore(recordQuery, enabled)
    const { records } = useDatastore(recordInfo?.data?.records)

    React.useEffect(() => {
        if (tableName && enabled && columnInfo.isError) {
            columnInfo.refetch()
        }
        // eslint-disable-next-line react-hooks/exhaustive-deps
    }, [tableName, enabled])

    // when fetch error
    React.useEffect(() => {
        if (tableName && enabled && recordInfo.isError) {
            recordInfo.refetch()
        }
        // eslint-disable-next-line react-hooks/exhaustive-deps
    }, [tableName, enabled])

    // when table changed
    React.useEffect(() => {
        if (recordQuery.tableName && !recordInfo.isLoading) {
            recordInfo.refetch()
        }
        // eslint-disable-next-line react-hooks/exhaustive-deps
    }, [recordQuery])

    return {
        recordQuery,
        columnInfo: recordInfo,
        recordInfo,
        columnTypes: columnInfo.data?.columnTypes as RecordSchemaT[],
        records,
    }
}
