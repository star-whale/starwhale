import React from 'react'
import { Pagination } from 'baseui/pagination'
import { IPaginationProps } from '../../types'
import { useStoreApi } from '../../hooks/useStore'

function AutoPagination({
    page,
    onPageChange,
    total = 1,
}: {
    page: {
        pageNum?: number
        pageSize?: number
    }
    total: number
    onPageChange?: (page: IPaginationProps) => void
}) {
    const maxPage = React.useRef(total)

    if (total > maxPage.current) {
        maxPage.current = total
    }

    const paginationProps = React.useMemo(() => {
        if (!page) return {}

        return {
            start: page.pageNum,
            count: page.pageSize,
            afterPageChange: (next: any) => {
                onPageChange?.(next)
            },
        }
    }, [page, onPageChange])

    if (!page) return null

    return (
        <div
            style={{
                display: 'flex',
                alignItems: 'center',
                marginTop: 20,
                height: '30px',
            }}
        >
            <div
                style={{
                    flexGrow: 1,
                }}
            />
            <Pagination
                overrides={{
                    MaxLabel: {
                        component: () => <></>,
                    },
                }}
                size='mini'
                numPages={maxPage.current}
                currentPage={paginationProps.start ?? 1}
                onPageChange={({ nextPage }) => {
                    // if (paginationProps.onPageChange) {
                    //     paginationProps.onPageChange(nextPage)
                    // }
                    if (paginationProps.afterPageChange) {
                        paginationProps.afterPageChange({
                            ...page,
                            pageNum: nextPage,
                        } as any)
                    }
                }}
            />
        </div>
    )
}

function AutoStorePagination() {
    const store = useStoreApi()
    const { page, onPageChange, records } = store.getState()

    if (!page) return null

    return (
        <AutoPagination
            page={page}
            onPageChange={onPageChange as any}
            total={records && records?.length === page.pageSize ? page.pageNum + 1 : page.pageNum}
        />
    )
}

export { AutoPagination }
export default AutoStorePagination
