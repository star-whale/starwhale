import { DatastorePageT } from '@starwhale/core/datastore'
import { getQuery, getScanQuery } from '@starwhale/core/datastore/hooks/useDatastoreQueryParams'
import React from 'react'

export type DatastorePagePropsT = {
    pageNum?: number
    pageSize?: number
    sortBy?: string
    sortDirection?: 'ASC' | 'DESC'
    queries?: any[]
}

function useDatastorePage({
    pageNum = 1,
    pageSize = 100,
    sortBy,
    sortDirection = 'DESC',
    queries,
}: DatastorePagePropsT) {
    const [page, setPage] = React.useState<DatastorePageT>({} as any)

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

    return {
        page: $page,
        setPage: React.useCallback(
            (page: DatastorePageT) => {
                setPage(page)
            },
            [setPage]
        ),
        getScanParams: React.useCallback(
            (tables, options = {}) =>
                getScanQuery(tables, {
                    ...$page,
                    ...options,
                }),
            [$page]
        ),
        getQueryParams: React.useCallback(
            (tableName?: string, options = {}) =>
                getQuery({
                    tableName,
                    options: {
                        ...$page,
                        ...options,
                    },
                }),
            [$page]
        ),
    }
}

export { useDatastorePage }
export default useDatastorePage
