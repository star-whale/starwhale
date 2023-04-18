import React, { useEffect, useMemo } from 'react'
import { useListDatastoreTables } from './useFetchDatastore'
import { tablesOfEvaluation } from '../utils'

export function useDatastoreTablesByPrefix(prefix: string) {
    const allTables = useListDatastoreTables({ prefix })

    useEffect(() => {
        if (prefix) {
            allTables.refetch()
        }
        // eslint-disable-next-line react-hooks/exhaustive-deps
    }, [prefix])

    const tables = React.useMemo(() => {
        return allTables.data?.tables?.sort((a, b) => (a > b ? 1 : -1)) ?? []
    }, [allTables])

    return {
        isSuccess: allTables.isSuccess,
        isLoading: allTables.isLoading,
        names: tables,
        tables: React.useMemo(
            () =>
                tables.map((table: string) => {
                    return {
                        short: table.replace(`${prefix}`, ''),
                        name: table,
                    }
                }),
            [tables, prefix]
        ),
    }
}

export default function useDatastoreTables(projectId: string, jobUuid: string) {
    const queryAllTables = useMemo(() => {
        if (!projectId || !jobUuid) return undefined
        return {
            prefix: tablesOfEvaluation(projectId, jobUuid),
        }
    }, [projectId, jobUuid])

    return useDatastoreTablesByPrefix(queryAllTables?.prefix as string)
}
