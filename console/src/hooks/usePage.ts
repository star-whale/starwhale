import { useCallback, useMemo } from 'react'
import { IListQuerySchema } from '@/domain/base/schemas/list'
import { IQueryArgs, IUpdateQueryArgs, useQueryArgs } from './useQueryArgs'

export function usePage(opt?: {
    query?: IQueryArgs
    updateQuery?: IUpdateQueryArgs
    defaultCount?: number
}): [IListQuerySchema & Record<string, any>, (page: IListQuerySchema & Record<string, any>) => void] {
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

    const { pageNum: pageStr = '1', pageSize: pageSizeStr, search, q, sort: sortBy, ...rest } = query
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
            }),
            [q, search, sortBy, pageNum, pageSize, rest]
        ),
        useCallback(
            ({ pageNum, pageSize, search, ...newRest }) => {
                updateQuery?.({
                    ...rest,
                    ...newRest,
                    pageNum: pageNum,
                    pageSize: pageSize,
                    search: search,
                })
            },
            [updateQuery, rest]
        ),
    ]
}
