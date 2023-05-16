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

const selector = (state: IGridState) => ({
    onIncludedRowsChange: state.onIncludedRowsChange,
    onRowHighlightChange: state.onRowHighlightChange,
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
    emptyMessage,
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
    const { onIncludedRowsChange, onRowHighlightChange } = useStore(selector)
    const store = useStoreApi()
    // @FIXME
    useDirectStoreUpdater(
        'getColumns',
        useCallback(() => columns, [columns]),
        store.setState
    )
    useDirectStoreUpdater('rows', rows, store.setState)

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
        onPreview,
        onPreviewClose,
    } = useGrid()

    console.log(preview)

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
                    previewable={previewable}
                    rawColumns={$columns}
                    emptyMessage={emptyMessage ?? <BusyPlaceholder type='notfound' />}
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
                    onPreview={onPreview}
                    // controlRef={controlRef}
                    // filters={$filtersEnabled}
                />
                {columns?.length === 0 && (emptyColumnMessage ?? <BusyPlaceholder type='notfound' />)}
            </div>
            <Preview
                preview={preview.record}
                previewKey={preview.columnKey}
                isFullscreen={!!preview.record}
                setIsFullscreen={onPreviewClose}
            />
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
