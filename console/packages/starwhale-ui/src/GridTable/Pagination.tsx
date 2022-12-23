import React from 'react'
import { Pagination as BasePagination, SIZE as PaginationSize } from 'baseui/pagination'
import { usePage } from '@/hooks/usePage'

export default function Pagination(paginationProps: any) {
    const [page, setPage] = usePage()

    if (!paginationProps?.total) return <></>

    return (
        <div
            style={{
                display: 'flex',
                alignItems: 'center',
                marginTop: 20,
            }}
        >
            <div
                style={{
                    flexGrow: 1,
                }}
            />
            <BasePagination
                size={PaginationSize.mini}
                numPages={
                    paginationProps.total && paginationProps.count
                        ? Math.ceil(paginationProps.total / Math.max(paginationProps.count, 1))
                        : 0
                }
                currentPage={paginationProps.start ?? 1}
                onPageChange={({ nextPage }: any) => {
                    if (paginationProps.onPageChange) {
                        paginationProps.onPageChange(nextPage)
                    }
                    if (paginationProps.afterPageChange) {
                        setPage({
                            ...page,
                            pageNum: nextPage,
                        })
                        paginationProps.afterPageChange(nextPage)
                    }
                }}
            />
        </div>
    )
}
