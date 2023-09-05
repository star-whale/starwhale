import React from 'react'
import { useStoreApi } from '../../hooks/useStore'
import { Pagination } from '@starwhale/ui/Pagination'

function AutoStorePagination() {
    const store = useStoreApi()
    const { page, onPageChange, records } = store.getState()

    const max = records && records?.length === page?.pageSize ? page?.pageNum + 1 : page?.pageNum

    const paginationProps = {
        page,
        start: page?.pageNum,
        count: page?.pageSize,
        maxPage: max,
        setPage: onPageChange,
    }

    if (!page) return null

    return <Pagination {...paginationProps} />
}

export { AutoStorePagination }
export default AutoStorePagination
