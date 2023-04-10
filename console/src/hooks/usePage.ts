import { useCallback, useMemo } from 'react'
import { IListQuerySchema } from '@/domain/base/schemas/list'
import { IQueryArgs, IUpdateQueryArgs, useQueryArgs } from './useQueryArgs'

export function usePage(opt?: {
    query?: IQueryArgs
    updateQuery?: IUpdateQueryArgs
    defaultCount?: number
}): [IListQuerySchema, (page: IListQuerySchema) => void] {
    const { query: query_, updateQuery: updateQuery_ } = opt ?? {}
    const { query: query0, updateQuery: updateQuery0 } = useQueryArgs()

    let query = query_
    if (!query) {
        query = query0
    }

    let updateQuery = updateQuery_
    if (!updateQuery) {
        updateQuery = updateQuery0
    }

    const { pageNum: pageStr = '1', pageSize: pageSizeStr, search, q, sort: sortBy, sort_asc: sortAsc, ...rest } = query
    let pageNum = parseInt(pageStr, 10)
    // eslint-disable-next-line no-restricted-globals
    if (isNaN(pageNum) || pageNum <= 0) {
        pageNum = 1
    }

    let pageSize = parseInt(pageSizeStr, 10)
    // eslint-disable-next-line no-restricted-globals
    if (isNaN(pageSize) || pageSize <= 0) {
        pageSize = 10
    }

    return [
        useMemo(
            () => ({
                ...rest,
                pageNum,
                pageSize,
                search,
                q,
                sort: sortBy,
                sort_asc: sortAsc === 'true',
            }),
            [q, search, sortAsc, sortBy, pageNum, pageSize, rest]
        ),
        useCallback(
            (newPage) => {
                updateQuery?.({
                    ...rest,
                    pageNum: newPage.pageNum,
                    pageSize: newPage.pageSize,
                    search: newPage.search,
                })
            },
            [updateQuery, rest]
        ),
    ]
}
