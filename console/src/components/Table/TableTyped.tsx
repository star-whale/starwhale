/* eslint-disable */
import React from 'react'
import { Table as TableSemantic, TableProps as BaseTableProps } from 'baseui/table-semantic'
import { Pagination, SIZE as PaginationSize } from 'baseui/pagination'
import { Skeleton } from 'baseui/skeleton'
import { FiInbox } from 'react-icons/fi'
import useTranslation from '@/hooks/useTranslation'
import Text from '@/components/Text'
import { usePage } from '@/hooks/usePage'
import { IPaginationProps } from '@/components/Table/IPaginationProps'
import { StatefulTooltip } from 'baseui/tooltip'
import {
    StatefulDataTable,
    NumericalColumn,
    StringColumn,
    BatchActionT,
    RowActionT,
    CustomColumn,
} from 'baseui/data-table'

export interface ITableProps extends BaseTableProps {
    paginationProps?: IPaginationProps
}

export default function TableTyped({ isLoading, columns, data, overrides, paginationProps }: ITableProps) {
    const [t] = useTranslation()
    const [page, setPage] = usePage()

    const renderCell = (props: any) => {
        return (
            <StatefulTooltip accessibilityType='tooltip' content={props?.value}>
                <span>{props?.value?.toFixed(4)}</span>
            </StatefulTooltip>
        )
    }

    const mapDataToValue = (data: any, key: string) => data?.key

    const $columns = [
        StringColumn({
            title: t('Label'),
            mapDataToValue,
        }),
        // CustomColumn({
        //     title: t('Precision'),
        //     renderCell,
        //     mapDataToValue,
        // }),
        // CustomColumn({
        //     title: t('Recall'),
        //     renderCell,
        //     mapDataToValue: (data) => data.recall,
        // }),
        // CustomColumn({
        //     title: t('F1-score'),
        //     renderCell,
        //     mapDataToValue: (data) => data['f1-score'],
        // }),
        // NumericalColumn({
        //     title: t('Support'),
        //     mapDataToValue: (data) => data.support ?? 0,
        // }),
    ]

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
                    TableHeadCell: {
                        style: {
                            backgroundColor: 'var(--color-brandTableHeaderBackground)',
                            fontWeight: 'bold',
                            borderBottom: 'none',
                            fontSize: 14,
                            lineHeight: '16px',
                            padding: '15px 28px',
                        },
                    },
                    TableHeadRow: {
                        style: {
                            borderRadius: '4px',
                        },
                    },
                    TableBodyCell: {
                        style: {
                            padding: '0px 28px',
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
                        <FiInbox size={30} />
                        <Text>{t('no data')}</Text>
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
