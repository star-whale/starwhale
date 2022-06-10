/* eslint-disable */
import React, { useRef, useState } from 'react'
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
import _ from 'lodash'
import useResizeObserver from '@/hooks/window/useResizeObserver'

export interface ITableProps extends BaseTableProps {
    batchActions?: BatchActionT[]
    rowActions?: RowActionT[]
    paginationProps?: IPaginationProps
}

export default function TableTyped({
    isLoading,
    columns = [],
    data = [],
    overrides,
    paginationProps,
    batchActions = [],
    rowActions = [],
}: ITableProps) {
    const [t] = useTranslation()
    const [page, setPage] = usePage()
    const [key, setKey] = useState(0)
    const wrapperRef = useRef<HTMLDivElement>(null)
    const [width, setWidth] = useState(wrapperRef?.current?.offsetWidth)

    useResizeObserver((entries) => {
        if (entries[0].contentRect?.width !== width) {
            setWidth(entries[0].contentRect?.width)
            setKey(key + 1)
        }
    }, wrapperRef)

    const renderCell = (props: any) => {
        return (
            <StatefulTooltip accessibilityType='tooltip' content={props?.value}>
                <span>{props?.value}</span>
            </StatefulTooltip>
        )
    }

    const $columns = columns.map((raw: any, index) => {
        let column = raw
        let rowItem = data?.[0]?.[index]

        const initColumns = {
            title: raw,
            sortable: !React.isValidElement(data?.[0]?.[index]),
            sortFn: function (a: any, b: any) {
                return a.localeCompare(b)
            },
            mapDataToValue: (item: any) => item[index],
        }

        // fit for sample column
        if (typeof raw == 'string') {
            if (_.isString(rowItem)) {
                return StringColumn({
                    ...initColumns,
                    ...column,
                })
            } else if (React.isValidElement(rowItem)) {
                return CustomColumn({
                    ...initColumns,
                    renderCell: (props: any) => props.value,
                })
            }
        }

        switch (column.type) {
            case 'string':
                return StringColumn({
                    ...initColumns,
                    ...column,
                })
            case 'number':
                return NumericalColumn({
                    ...initColumns,
                    ...column,
                })
            default:
            case 'custom':
                return CustomColumn({
                    ...initColumns,
                    filterable: true,
                    buildFilter: function (params: any) {
                        return function (data: any) {
                            return params.selection.has(data)
                        }
                    },
                    renderCell: (props: any) => props.value,
                    ...column,
                })
        }
    })

    const $rows = data.map((raw, index) => {
        return {
            id: index + '',
            data: raw,
        }
    })

    return (
        <>
            <div
                style={{ width: '100%', minHeight: 200, height: `${120 + $rows.length * 36}px` }}
                ref={wrapperRef}
                key={key}
            >
                <StatefulDataTable
                    isLoading={!!isLoading}
                    batchActions={batchActions}
                    rowActions={rowActions}
                    columns={$columns}
                    rows={$rows}
                    resizableColumnWidths={true}
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
                    loadingMessage={() => <Skeleton rows={3} height='100px' width='100%' animation />}
                    emptyMessage={() => (
                        <div
                            style={{
                                display: 'flex',
                                flexDirection: 'column',
                                alignItems: 'center',
                                justifyContent: 'center',
                                gap: 8,
                            }}
                        >
                            <FiInbox size={30} />
                            <Text>{t('no data')}</Text>
                        </div>
                    )}
                />
            </div>
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
