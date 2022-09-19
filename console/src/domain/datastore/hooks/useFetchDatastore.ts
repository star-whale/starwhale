import React from 'react'
import { useQuery } from 'react-query'
import qs from 'qs'
import { IListQuerySchema } from '@/domain/base/schemas/list'
import { scanTable, queryTable, listTables } from '../services/datastore'

export function useScanDatastore(query: any, enabled = false) {
    const info = useQuery(`scanDatastore:${qs.stringify(query)}`, () => scanTable(query), {
        refetchOnWindowFocus: false,
        enabled,
    })
    return info
}

export function useQueryDatastore(query: any, enabled = false) {
    const info = useQuery(`queryDatastore:${qs.stringify(query)}`, () => queryTable(query), {
        refetchOnWindowFocus: false,
        enabled,
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

export function useQueryDatasetList(tableName?: string, page?: IListQuerySchema, rawResult = false) {
    const { start, limit } = React.useMemo(() => {
        const { pageNum = 1, pageSize = 10 } = page || {}

        return {
            start: (pageNum - 1) * pageSize ?? 0,
            limit: pageSize ?? 0,
        }
    }, [page])

    const info = useQueryDatastore({
        tableName,
        start,
        limit,
        rawResult,
        // https://github.com/star-whale/starwhale/pull/1128
        ignoreNonExistingTable: true,
    })

    React.useEffect(() => {
        if (tableName) {
            info.refetch()
        }
        // eslint-disable-next-line react-hooks/exhaustive-deps
    }, [tableName, start, limit])

    return info
}
