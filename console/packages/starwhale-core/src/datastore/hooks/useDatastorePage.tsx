import { DatastorePageT, getTableShortNamePrefix } from '@starwhale/core/datastore'
import { getQuery, getScanQuery } from '@starwhale/core/datastore/hooks/useDatastoreQueryParams'
import React from 'react'
import _ from 'lodash'

export type DatastorePagePropsT = {
    pageNum?: number
    pageSize?: number
    sortBy?: string
    sortDirection?: 'ASC' | 'DESC' | string
    queries?: any[]
    tableName?: string | string[]
    prefixFn?: (tableName: string) => string
}

function useDatastorePage({
    pageNum = 1,
    pageSize = 100,
    sortBy,
    sortDirection = 'DESC',
    queries,
    tableName,
    prefixFn = getTableShortNamePrefix,
}: DatastorePagePropsT) {
    const [page, setPage] = React.useState<DatastorePageT>({} as any)

    // eslint-disable-next-line @typescript-eslint/no-shadow
    const initPage = React.useCallback(({ sortBy, sortDirection, queries, pageNum, pageSize }) => {
        const sorts = sortBy
            ? [
                  {
                      columnName: sortBy,
                      descending: sortDirection === 'DESC',
                  },
              ]
            : []

        return {
            pageNum,
            pageSize,
            query: {
                orderBy: sorts,
            },
            filter: queries,
        }
    }, [])

    const rawPage = React.useMemo(
        () =>
            initPage({
                sortBy,
                sortDirection,
                queries,
                pageNum,
                pageSize,
            }) as DatastorePageT,
        [initPage, sortBy, sortDirection, queries, pageNum, pageSize]
    )

    const $page = React.useMemo(() => {
        return {
            ...rawPage,
            ...page,
        } as DatastorePageT
    }, [page, rawPage])

    const getScanParams = React.useCallback(
        (tables, options = {}) => {
            const normalizeTables = tables.map((t) => {
                if (_.isObject(t) && 'columnPrefix' in t) return t
                return {
                    tableName: t,
                    columnPrefix: [prefixFn?.(t), getTableShortNamePrefix(t)].filter(Boolean).join(' '),
                }
            })

            return getScanQuery(normalizeTables, {
                ...$page,
                ...options,
            })
        },
        [$page, prefixFn]
    )

    const getQueryParams = React.useCallback(
        (tableNameTmp?: string, options = {}) =>
            getQuery({
                tableName: tableNameTmp,
                options: {
                    ...$page,
                    ...options,
                },
            }),
        [$page]
    )

    return {
        page: $page,
        setPage: React.useCallback(
            (tmp: DatastorePageT) => {
                setPage(tmp)
            },
            [setPage]
        ),
        getScanParams,
        getQueryParams,
        params: React.useMemo(() => {
            if (!tableName || !tableName.length) return undefined
            if (Array.isArray(tableName) && tableName.length > 1) return getScanParams(tableName)
            if (Array.isArray(tableName)) return getQueryParams(tableName[0])
            return getQueryParams(tableName)
        }, [tableName, getScanParams, getQueryParams]),
    }
}

export { useDatastorePage }
export default useDatastorePage
