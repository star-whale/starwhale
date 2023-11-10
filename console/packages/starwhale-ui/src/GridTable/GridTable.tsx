import React, { useCallback, useRef } from 'react'
import { Skeleton } from 'baseui/skeleton'
import { areEqual } from 'react-window'
import { useStyletron } from 'baseui'
import { createUseStyles } from 'react-jss'
import cn from 'classnames'
import { IContextGridTable, IGridState, ITableProps } from './types'
import { BusyPlaceholder } from '../BusyLoaderWrapper'
import { useStore, useStoreApi } from './hooks/useStore'
import { StoreProvider } from './store/StoreProvider'
import StoreUpdater, { useDirectStoreUpdater } from './store/StoreUpdater'
import useGrid from './hooks/useGrid'
import { DataTable } from '../base/data-table/data-custom-table'
import useOnInitHandler from './hooks/useOnInitHandler'
import Preview from '../GridDatastoreTable/components/Preview'
import AutoStorePagination from './components/Pagination/AutoPagination'

const useStyles = createUseStyles({
    table: {
        // do not add overflow hidden here, toolbar will be blokced
        'width': '100%',
        'height': '100%',
        'position': 'relative',
        'flex': 1,
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
        'display': 'flex',
        'flexDirection': 'column',
        '& .table-columns-pinned': {
            borderRight: '1px solid rgb(207, 215, 230)',
        },
    },
})

const loadingMessage: any = () => (
    <Skeleton
        overrides={{
            Root: {
                style: {
                    paddingTop: '10px',
                    minHeight: '200px',
                },
            },
        }}
        rows={10}
        width='100%'
        animation
    />
)
const defaultEmptyMessage: any = () => <BusyPlaceholder type='notfound' style={{ minHeight: '300px' }} />

const selector = (state: IGridState) => ({
    onIncludedRowsChange: state.onIncludedRowsChange,
    onRowHighlightChange: state.onRowHighlightChange,
    onRemove: state.onRemove,
    getId: state.getId,
    rowSelectedIds: state.rowSelectedIds,
})

function GridTable({
    isLoading,
    columns,
    rows,
    rowActions,
    compareable = false,
    selectable = false,
    queryinline = false,
    previewable = false,
    paginationable = false,
    removable = false,
    emptyMessage = defaultEmptyMessage,
    emptyColumnMessage,
    resizableColumnWidths = true,
    rowHighlightIndex = -1,
    rowHeight = 44,
    headlineHeight = 0,
    onInit,
    children,
}: ITableProps) {
    useOnInitHandler(onInit)

    const wrapperRef = useRef<HTMLDivElement>(null)
    const [, theme] = useStyletron()
    const styles = useStyles({ theme })
    const { onRemove, onIncludedRowsChange, onRowHighlightChange, getId } = useStore(selector)
    const store = useStoreApi()
    // @FIXME inline set move to  store updater ?
    useDirectStoreUpdater(
        'getColumns',
        useCallback(() => columns, [columns]),
        store.setState
    )
    useDirectStoreUpdater('rows', rows, store.setState)
    useDirectStoreUpdater('wrapperRef', wrapperRef, store.setState)

    const {
        selectedRowIds,
        columns: $columns,
        rows: $rows,
        sortIndex,
        sortDirection,
        textQuery,
        onSelectMany,
        onSelectNone,
        onSelectOne,
        isRowSelected,
        isSelectedAll,
        isSelectedIndeterminate,
        preview,
    } = useGrid()

    const paginationHeight = paginationable ? 50 : 0

    return (
        <div
            className={cn(
                styles.table,
                styles.tablePinnable,
                compareable ? styles.tableCompareable : undefined,
                'not-prose'
            )}
            ref={wrapperRef}
        >
            {children}
            <div
                data-type='table-wrapper'
                style={{
                    width: '100%',
                    height: `calc(100% - ${headlineHeight + paginationHeight}px)`,
                    flex: 1,
                    display: 'flex',
                    flexDirection: 'column',
                }}
            >
                <DataTable
                    columns={$columns}
                    selectable={selectable}
                    compareable={compareable}
                    queryinline={queryinline}
                    previewable={previewable}
                    removable={removable}
                    rawColumns={$columns}
                    emptyMessage={emptyMessage}
                    loading={isLoading}
                    loadingMessage={loadingMessage}
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
                    selectedRowIds={selectedRowIds as Set<string | number>}
                    sortDirection={sortDirection}
                    sortIndex={sortIndex}
                    textQuery={textQuery}
                    onPreview={preview.onPreview as any}
                    onRemove={onRemove}
                    getId={getId}
                    // controlRef={controlRef}
                    // filters={$filtersEnabled}
                />
                {columns?.length === 0 && (emptyColumnMessage ?? <BusyPlaceholder type='notfound' />)}
            </div>
            {paginationable && <AutoStorePagination />}
            <Preview {...preview} isFullscreen={!!preview.current} setIsFullscreen={preview.onPreviewClose} />
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
            <StoreUpdater {...rest} />
            <MemoGridTable {...rest}>{children}</MemoGridTable>
        </StoreProvider>
    )
}
