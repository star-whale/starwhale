import React from 'react'
import { useQuery } from 'react-query'
import qs from 'qs'
import { IListQuerySchema } from '@/domain/base/schemas/list'
import { scanTable, queryTable } from '../services/datastore'

export function useScanDatastore(query: any) {
    const info = useQuery(`scanDatastore:${qs.stringify(query)}`, () => scanTable(query), {
        refetchOnWindowFocus: false,
        enabled: false,
    })
    return info
}

export function useQueryDatastore(query: any) {
    const info = useQuery(`queryDatastore:${qs.stringify(query)}`, () => queryTable(query), {
        refetchOnWindowFocus: false,
        enabled: false,
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
    })

    React.useEffect(() => {
        if (tableName) {
            info.refetch()
        }
        // eslint-disable-next-line react-hooks/exhaustive-deps
    }, [tableName, start, limit])

    return info
}
