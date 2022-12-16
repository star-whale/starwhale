import React, { useEffect, useMemo } from 'react'
import { useListDatastoreTables } from './useFetchDatastore'
import { tablesOfEvaluation } from '../utils'

export default function useDatastoreTables(projectName: string, jobUuid: string) {
    const queryAllTables = useMemo(() => {
        if (!projectName || !jobUuid) return {}
        return {
            prefix: tablesOfEvaluation(projectName, jobUuid),
        }
    }, [projectName, jobUuid])

    // eslint-disable-next-line @typescript-eslint/no-use-before-define
    return useDatastoreTablesByPrefix(queryAllTables?.prefix as string)
}

export function useDatastoreTablesByPrefix(prefix: string) {
    const allTables = useListDatastoreTables({ prefix })

    useEffect(() => {
        if (prefix) {
            allTables.refetch()
        }
        // eslint-disable-next-line react-hooks/exhaustive-deps
    }, [prefix])

    const tables = React.useMemo(() => {
        return allTables.data?.tables?.sort((a, b) => (a > b ? 1 : -1)).filter((v) => !v.includes('results')) ?? []
    }, [allTables])

    return {
        names: tables,
        tables: tables.map((table) => {
            return {
                short: table.replace(`${prefix}`, ''),
                name: table,
            }
        }),
    }
}

export default function useDatastoreTables(projectName: string, jobUuid: string) {
    const queryAllTables = useMemo(() => {
        if (!projectName || !jobUuid) return undefined
        return {
            prefix: tablesOfEvaluation(projectName, jobUuid),
        }
    }, [projectName, jobUuid])

    return useDatastoreTablesByPrefix(queryAllTables?.prefix as string)
}
