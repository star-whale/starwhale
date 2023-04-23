import React, { useEffect } from 'react'
import { useListDatastoreTables } from './useFetchDatastore'

export function useFetchDatastoreAllTables(prefix: string) {
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

export default useFetchDatastoreAllTables
