import React, { useRef, useMemo, useEffect } from 'react'
import { Skeleton } from 'baseui/skeleton'
import { areEqual } from 'react-window'
import { useStyletron } from 'baseui'
import { createUseStyles } from 'react-jss'
import cn from 'classnames'
import { IContextGridTable, ITableProps } from './types'
import { StoreProvider, useTableContext } from './StoreContext'
import Pagination from './Pagination'
import { BusyPlaceholder } from '../BusyLoaderWrapper'
import { StatefulDataTable } from '../base/data-table'

const useStyles = createUseStyles({
    table: {
        'width': '100%',
        'height': '100%',
        '& .baseui-table-cell-content': {},
        '& .column-cell .string-cell': {
            overflow: 'hidden',
            textOverflow: 'ellipsis',
            whiteSpace: 'nowrap',
        },
        '& .table-row': {
            '&:hover': {},
        },
        '& .table-columns-pinned': {
            backgroundColor: '#FFF',
        },
        '& .table-row--hovering': {
            backgroundColor: '#EFEEF5',
        },
        '& .table-row--hovering .column-cell > *': {
            backgroundColor: '#EFEEF5 !important',
        },
    },
    tableCompareable: {
        '& .table-cell--last': {},
    },
    tablePinnable: {
        '& .table-columns-pinned .table-row .table-cell:last-child': {
            borderRight: '1px solid rgb(207, 215, 230); ',
        },
        '& .table-headers-pinned > div:last-child': {
            borderRight: '1px solid rgb(207, 215, 230)',
        },
    },
})

function GridTable({
    isLoading,
    columns = [],
    data = [],
    paginationProps,
    batchActions = [],
    rowActions = [],
    onSelectionChange,
    searchable = false,
    filterable = false,
    queryable = false,
    columnable = false,
    compareable = false,
    viewable = false,
}: ITableProps) {
    const wrapperRef = useRef<HTMLDivElement>(null)
    const api = useTableContext()
    const store = api()
    const $rows = useMemo(
        () =>
            data.map((raw, index) => {
                return {
                    id: raw.id ?? index.toFixed(),
                    data: raw,
                }
            }),
        [data]
    )

    const $filters = useMemo(() => {
        return store.currentView?.filters
    }, [store])

    useEffect(() => {
        if (!store.isInit && columns.length > 0) {
            api.setState({
                isInit: true,
                currentView: {
                    name: '',
                    filters: [],
                    selectedIds: columns.map((v) => v.key) ?? [],
                    sortedIds: [],
                    pinnedIds: [],
                },
            })
        }
    }, [store.isInit, columns, api])

    const [, theme] = useStyletron()
    const styles = useStyles({ theme })

    return (
        <>
            <div
                className={cn(styles.table, styles.tablePinnable, compareable ? styles.tableCompareable : undefined)}
                ref={wrapperRef}
            >
                <StatefulDataTable
                    store={store}
                    useStore={api}
                    resizableColumnWidths
                    onSelectionChange={onSelectionChange}
                    initialFilters={$filters}
                    searchable={searchable}
                    filterable={filterable}
                    queryable={queryable}
                    columnable={columnable}
                    compareable={compareable}
                    viewable={viewable}
                    loading={!!isLoading}
                    batchActions={batchActions}
                    rowActions={rowActions}
                    columns={columns}
                    rows={$rows}
                    // @ts-ignore
                    loadingMessage={() =>
                        (
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
                        ) as any
                    }
                    // @ts-ignore
                    emptyMessage={() => (<BusyPlaceholder type='notfound' />) as any}
                />
            </div>
            <Pagination {...paginationProps} />
        </>
    )
}

export const MemoGridTable = React.memo(GridTable, areEqual)

export default function ContextGridTable({
    storeKey = 'table',
    initState = {},
    store = undefined,
    ...rest
}: IContextGridTable) {
    return (
        <StoreProvider initState={initState} storeKey={storeKey} store={store}>
            <MemoGridTable {...rest} />
        </StoreProvider>
    )
}
