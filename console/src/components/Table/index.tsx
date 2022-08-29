import React from 'react'
import { Table as TableSemantic, TableProps as BaseTableProps } from 'baseui/table-semantic'
import { Pagination, SIZE as PaginationSize } from 'baseui/pagination'
import { Skeleton } from 'baseui/skeleton'
import useTranslation from '@/hooks/useTranslation'
import { usePage } from '@/hooks/usePage'
import { IPaginationProps } from '@/components/Table/IPaginationProps'
import BusyPlaceholder from '../BusyLoaderWrapper/BusyPlaceholder'

export interface ITableProps extends BaseTableProps {
    paginationProps?: IPaginationProps
}

export default function Table({ isLoading, columns, data, overrides, paginationProps }: ITableProps) {
    const [t] = useTranslation()
    const [page, setPage] = usePage()

    return (
        <>
            <TableSemantic
                isLoading={isLoading}
                columns={columns}
                data={data}
                overrides={{
                    TableBodyRow: {
                        style: {
                            cursor: 'pointer',
                            borderRadius: '4px',
                        },
                        props: {
                            // eslint-disable-next-line
                            onClick: (e: React.MouseEvent) => {
                                // e.currentTarget.querySelector('a')?.click()
                            },
                        },
                    },
                    // @ts-ignore
                    TableHeadCell: {
                        style: {
                            backgroundColor: 'var(--color-brandTableHeaderBackground)',
                            fontWeight: 'bold',
                            borderBottomWidth: 0,
                            fontSize: 14,
                            lineHeight: '16px',
                            paddingTop: '15px',
                            paddingBottom: '15px',
                            paddingLeft: '20px',
                            paddingRight: '20px',
                        },
                    },
                    TableHeadRow: {
                        style: {
                            borderRadius: '4px',
                        },
                    },
                    TableBodyCell: {
                        style: {
                            paddingTop: 0,
                            paddingBottom: 0,
                            paddingLeft: '20px',
                            paddingRight: '20px',
                            lineHeight: '44px',
                        },
                    },
                    ...overrides,
                }}
                loadingMessage={<Skeleton rows={3} height='100px' width='100%' animation />}
                emptyMessage={
                    <div
                        style={{
                            display: 'flex',
                            flexDirection: 'column',
                            alignItems: 'center',
                            justifyContent: 'center',
                            gap: 8,
                            height: 100,
                        }}
                    >
                        <BusyPlaceholder type='notfound' />
                    </div>
                }
            />
            {paginationProps && (
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
                    <Pagination
                        size={PaginationSize.mini}
                        numPages={
                            paginationProps.total && paginationProps.count
                                ? Math.ceil(paginationProps.total / Math.max(paginationProps.count, 1))
                                : 0
                        }
                        currentPage={paginationProps.start ?? 1}
                        onPageChange={({ nextPage }) => {
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
            )}
        </>
    )
}
