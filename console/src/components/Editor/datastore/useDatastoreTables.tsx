import { useListDatastoreTables } from '@/domain/datastore/hooks/useFetchDatastore'
import { showTableName, tableNameOfSummary, tablesOfEvaluation } from '@/domain/datastore/utils'
import React, { useEffect, useMemo } from 'react'

export default function useDatastoreTables(projectName: string, jobUuid: string) {
    const queryAllTables = useMemo(() => {
        if (!projectName || !jobUuid) return ''
        return {
            prefix: tablesOfEvaluation(projectName, jobUuid),
        }
    }, [projectName, jobUuid])
    const allTables = useListDatastoreTables(queryAllTables)

    useEffect(() => {
        if (projectName && jobUuid) {
            allTables.refetch()
        }
        // eslint-disable-next-line react-hooks/exhaustive-deps
    }, [projectName, jobUuid])

    const tables = React.useMemo(() => {
        // const names = []
        // if (projectName) names.push(tableNameOfSummary(projectName))

        return [
            // ...names,
            // @FIXME hard code remove results
            ...(allTables.data?.tables?.sort((a, b) => (a > b ? 1 : -1)).filter((v) => !v.includes('results')) ?? []),
        ]
    }, [allTables, projectName])

    return {
        names: tables,
        tables: tables.map((table) => {
            // @FIXME hard code remove summary replace
            const short = table.replace(`${tablesOfEvaluation(projectName, jobUuid)}/`, '')
            // .replace('project/mnist-exp/eval/', '')
            return {
                short,
                name: table,
            }
        }),
    }
}
