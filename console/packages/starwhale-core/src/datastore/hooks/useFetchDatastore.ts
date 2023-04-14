import React, { useMemo } from 'react'
import { useQuery } from 'react-query'
import qs from 'qs'
import { scanTable, queryTable, listTables, exportTable } from '../services/datastore'
import { QueryTableRequest } from '../schemas/datastore'

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
