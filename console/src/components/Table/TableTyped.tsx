/* eslint-disable */
import React, { useRef, useState, useMemo } from 'react'
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
    CategoricalColumn,
    NumericalColumn,
    StringColumn,
    CustomColumn,
    Types,
} from '@/components/data-table'
import _, { uniqueId } from 'lodash'
// import useResizeObserver from '@/hooks/window/useResizeObserver'
import CategoricalTagsColumn from '../data-table/column-categorical-tags'
import { useCallback } from 'react'
import { useTableConfig, useTableViewConfig } from '@/hooks/useTableConfig'
import { areEqual } from 'react-window'
import type { ColumnT, ConfigT, RowT } from '../data-table/types'
import { useUID, useUIDSeed } from 'react-uid'
import useStore from '../data-table/store'
import { useEffect } from 'react'
import { useStyletron } from 'baseui'
import { createUseStyles } from 'react-jss'
import cn from 'classnames'
import { useCurrentThemeType } from '@/hooks/useCurrentThemeType'

const useStyles = createUseStyles({
    table: {
        '& .baseui-table-cell-content': {},
    },
    tableCompareable: {
        '& table-cell--last': {
            borderBottom: '1px solid #2B65D9',
        },
    },
    tablePinnable: {
        '& .table-columns-pinned .table-row .table-cell:last-child': {
            borderRight: '1px solid rgb(207, 215, 230);',
        },
        '& .table-headers-pinned > div:last-child': {
            borderRight: '1px solid rgb(207, 215, 230)',
        },
    },
})

export interface ITableProps extends BaseTableProps {
    batchActions?: Types.BatchActionT[]
    rowActions?: Types.RowActionT[]
    paginationProps?: IPaginationProps
    onColumnSave?: (props: any) => void
    onSelectionChange?: (rows: RowT[]) => void
    filterable?: boolean
    searchable?: boolean
    columnable?: boolean
    compareable?: boolean
    viewable?: boolean
    id?: string
    data: any[]
}

export function TableTyped({
    isLoading,
    columns = [],
    data = [],
    overrides,
    paginationProps,
    batchActions = [],
    rowActions = [],
    onColumnSave,
    onSelectionChange,
    searchable = false,
    filterable = false,
    columnable = false,
    compareable = false,
    viewable = false,
    id,
}: ITableProps) {
    const [t] = useTranslation()
    const [page, setPage] = usePage()
    const wrapperRef = useRef<HTMLDivElement>(null)
    console.log('【TableRendered】')

    let $columns = columns.map((raw: any, index) => {
        let column = raw

        if (typeof raw !== 'string') {
            return {
                ...column,
                fillWidth: false,
                // maxWidth: 200,
            }
        }

        // @ts-ignore
        let item = data?.[0]?.[index]
        if (typeof raw === 'string') {
            column = { type: 'string', title: raw, index, sortable: true }
        }
        if (React.isValidElement(item)) {
            column = { type: 'custom', title: raw, index, renderCell: (props: any) => <>{props.value}</> }
        }

        const initColumns = {
            pin: null,
            title: column.title,
            resizable: true,
            cellBlockAlign: 'center',
            index: column.index,
            // @ts-ignore
            sortable: !React.isValidElement(data?.[0]?.[index]),
            sortFn: function (a: any, b: any) {
                return a.localeCompare(b)
            },
            mapDataToValue: (item: any) => item[index],
            minWidth: 100,
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
            case 'categorical':
                return CategoricalColumn({
                    ...initColumns,
                    ...column,
                })
            case 'tags':
                return CategoricalTagsColumn({
                    ...initColumns,
                    ...column,
                })
        }
    })

    const $rows = useMemo(
        () =>
            data.map((raw, index) => {
                return {
                    id: index + '',
                    data: raw,
                }
            }),
        [data]
    )

    const ROW_HEIGHT = 44
    const store = useStore()
    const $filters = useMemo(() => {
        return store.currentView?.filters
    }, [store])

    useEffect(() => {
        if (!store.isInit)
            useStore.setState({
                isInit: true,
                currentView: {
                    name: '',
                    filters: [],
                    selectedIds: $columns.map((v) => v.key) ?? [],
                    sortedIds: [],
                    pinnedIds: [],
                },
            })
    }, [$columns])

    const styles = useStyles()

    return (
        <>
            <div
                // style={{ width: '100%', minHeight: 500, height: `${120 + Math.min($rows.length, 10) * ROW_HEIGHT}px` }}
                className={cn(styles.table, styles.tablePinnable, compareable ? styles.tableCompareable : undefined)}
                style={{ width: '100%', minHeight: 500, height: `calc(100vh - 270px)` }}
                ref={wrapperRef}
            >
                <StatefulDataTable
                    resizableColumnWidths
                    onSelectionChange={onSelectionChange}
                    initialFilters={$filters}
                    searchable={searchable}
                    filterable={filterable}
                    columnable={columnable}
                    viewable={viewable}
                    loading={!!isLoading}
                    batchActions={batchActions}
                    rowActions={rowActions}
                    columns={$columns}
                    rows={$rows}
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
                                borderBottomWidth: 0,
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
                    // @ts-ignore
                    loadingMessage={() => (
                        <Skeleton
                            overrides={{
                                Root: {
                                    style: {
                                        paddingTop: '10px',
                                    },
                                },
                            }}
                            rows={10}
                            width='100%'
                            animation
                        />
                    )}
                    // @ts-ignore
                    emptyMessage={() => (
                        <div
                            style={{
                                position: 'absolute',
                                top: '50%',
                                left: 0,
                                right: 0,
                                display: 'flex',
                                flexDirection: 'column',
                                alignItems: 'center',
                                justifyContent: 'center',
                                gap: 8,
                            }}
                        >
                            <FiInbox size={50} />
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

export default React.memo(TableTyped, areEqual)
