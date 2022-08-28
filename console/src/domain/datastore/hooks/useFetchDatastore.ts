import React from 'react'
import { useQuery } from 'react-query'
import qs from 'qs'
import { scanTable, queryTable } from '../services/datastore'
import { tableNameOfDataset } from '../utils'
import { IListQuerySchema } from '@/domain/base/schemas/list'

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

export function useQueryDatasetList(tableName?: string, page?: IListQuerySchema) {
    const info = useQueryDatastore({
        tableName,
        start: page?.pageNum ?? 0,
        limit: page?.pageSize ?? 0,
    })

    React.useEffect(() => {
        if (tableName) {
            info.refetch()
        }
    }, [tableName, page])

    return info
}
