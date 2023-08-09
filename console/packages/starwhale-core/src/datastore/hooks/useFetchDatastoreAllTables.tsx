import React, { useEffect } from 'react'
import { useListDatastoreTables } from './useFetchDatastore'
import { getTableShortName, getTableShortNamePrefix } from '../utils'

export function useFetchDatastoreAllTables(
    prefix: string,
    prefixes: {
        name: string
        prefix?: string
    }[]
) {
    const params = React.useMemo(() => {
        if (prefixes) {
            return {
                prefixes: prefixes.map((item) => item.name),
            }
        }
        if (prefix) return { prefix }
        return undefined
    }, [prefix, prefixes])

    const allTables = useListDatastoreTables(params)

    useEffect(() => {
        if (params) {
            allTables.refetch()
        }
        // eslint-disable-next-line react-hooks/exhaustive-deps
    }, [params])

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
                    const shortPrefix = prefixes?.find((item) => table.startsWith(item.name))?.prefix ?? ''
                    const shortName = prefix ? table.replace(`${prefix}`, '') : getTableShortName(table)
                    return {
                        short: [shortName, shortPrefix].filter(Boolean).join('@'),
                        name: table,
                    }
                }),
            [tables, prefix, prefixes]
        ),
    }
}

export default useFetchDatastoreAllTables
