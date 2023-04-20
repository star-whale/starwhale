import React, { useRef, useMemo } from 'react'
import { Skeleton } from 'baseui/skeleton'
import { areEqual } from 'react-window'
import { useStyletron } from 'baseui'
import { createUseStyles } from 'react-jss'
import cn from 'classnames'
import { IContextGridTable, ITableProps } from './types'
import Pagination from './Pagination'
import { BusyPlaceholder } from '../BusyLoaderWrapper'
import { StatefulDataTable } from '../base/data-table'
import { stateSelector } from '../base/data-table/store'
import { useStore, useStoreApi } from './hooks/useStore'
import { StoreProvider } from './store/StoreProvider'
import StoreUpdater, { useDirectStoreUpdater } from './store/StoreUpdater'
import useGrid from './hooks/useGrid'
import { DataTable } from '../base/data-table/data-custom-table'
import { ITableState } from './store'

const useStyles = createUseStyles({
    table: {
        'width': '100%',
        'height': '100%',
        'position': 'relative',
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
            backgroundColor: '#EBF1FF',
        },
        '& .table-row--hovering .table-cell': {
            backgroundColor: '#EBF1FF !important',
        },
        '& .table-row--hovering .column-cell > *': {
            backgroundColor: '#EBF1FF !important',
        },
    },
    tableCompareable: {
        '& .table-cell--last': {},
    },
    tablePinnable: {
        '& .table-columns-pinned': {
            borderRight: '1px solid rgb(207, 215, 230)',
        },
        '& .table-headers-pinned > div:last-child .header-cell': {
            borderRight: '1px solid rgb(207, 215, 230)',
        },
    },
})

const loadingMessage = () => (
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
)

const selector = (state: ITableState) => ({
    onIncludedRowsChange: state.onIncludedRowsChange,
    onRowHighlightChange: state.onRowHighlightChange,
})
function val(r: any) {
    if (r === undefined) return ''
    if (typeof r === 'object' && 'value' in r) {
        return typeof r.value === 'object' ? JSON.stringify(r.value, null) : r.value
    }

    return r
}

function GridTable({
    isLoading,
    columns,
    rows,
    data = [],
    paginationProps,
    rowActions,
    searchable = false,
    filterable = false,
    queryable = false,
    compareable = false,
    selectable = false,
    queryinline = false,
    onSave,
    onChange = () => {},
    emptyMessage,
    emptyColumnMessage,
    // todo
    getId = (record: any) => val(record.id),
    resizableColumnWidths = false,
    rowHighlightIndex = -1,
    rowHeight = 44,
    storeRef,
    onColumnsChange,
    headlineHeight = 60,
    children,
}: ITableProps) {
    const wrapperRef = useRef<HTMLDivElement>(null)
    const [, theme] = useStyletron()
    const styles = useStyles({ theme })
    const { onIncludedRowsChange, onRowHighlightChange } = useStore(selector)
    const store = useStoreApi()

    const {
        columns: defaultColumns,
        rows: defaultRows,
        sortIndex,
        sortDirection,
        textQuery,
        rowSelectedIds,
        onSelectMany,
        onSelectNone,
        onSelectOne,
        isRowSelected,
        isSelectedAll,
        isSelectedIndeterminate,
    } = useGrid()

    const $columns = useMemo(() => {
        return columns ?? defaultColumns ?? []
    }, [columns, defaultColumns])
    const $rows = useMemo(() => {
        return rows ?? defaultRows ?? []
    }, [rows, defaultRows])

    // @FIXME
    useDirectStoreUpdater('columns', $columns, store.setState)
    useDirectStoreUpdater('rows', $rows, store.setState)

    return (
        <div
            className={cn(styles.table, styles.tablePinnable, compareable ? styles.tableCompareable : undefined)}
            ref={wrapperRef}
        >
            {children}
            <div data-type='table-wrapper' style={{ width: '100%', height: `calc(100% - ${headlineHeight}px)` }}>
                <DataTable
                    columns={$columns}
                    selectable={selectable}
                    compareable={compareable}
                    queryinline={queryinline}
                    rawColumns={$columns}
                    emptyMessage={emptyMessage ?? <BusyPlaceholder type='notfound' />}
                    // filters={$filtersEnabled}
                    loading={isLoading}
                    loadingMessage={emptyMessage ?? loadingMessage}
                    onIncludedRowsChange={onIncludedRowsChange}
                    onRowHighlightChange={onRowHighlightChange}
                    isRowSelected={isRowSelected}
                    isSelectedAll={isSelectedAll}
                    isSelectedIndeterminate={isSelectedIndeterminate}
                    onSelectMany={onSelectMany}
                    onSelectNone={onSelectNone}
                    onSelectOne={onSelectOne}
                    resizableColumnWidths={resizableColumnWidths}
                    rowHighlightIndex={rowHighlightIndex}
                    rows={$rows}
                    rowActions={rowActions}
                    rowHeight={rowHeight}
                    selectedRowIds={rowSelectedIds}
                    sortDirection={sortDirection}
                    sortIndex={sortIndex}
                    textQuery={textQuery}
                    // controlRef={controlRef}
                />
                {columns?.length === 0 && (emptyColumnMessage ?? <BusyPlaceholder type='notfound' />)}
            </div>
        </div>
    )
}

export const MemoGridTable = React.memo(GridTable, areEqual)

export default function ContextGridTable({
    storeKey = 'table',
    initState = {},
    store = undefined,
    children,
    ...rest
}: IContextGridTable) {
    return (
        <StoreProvider initState={initState} storeKey={storeKey} store={store}>
            <StoreUpdater {...rest}>
                <MemoGridTable {...rest}>{children}</MemoGridTable>
            </StoreUpdater>
        </StoreProvider>
    )
}
