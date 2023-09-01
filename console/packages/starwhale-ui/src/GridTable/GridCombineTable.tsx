import React from 'react'
import { BusyPlaceholder, GridResizer } from '@starwhale/ui'
import ToolBar from '@starwhale/ui/GridTable/components/ToolBar'
import { useStore } from './hooks/useStore'
import { ITableProps, IContextGridTable, IGridState } from './types'
import { StoreProvider } from './store'
import { StoreUpdater } from './store/StoreUpdater'
import { MemoGridTable } from './GridTable'
import GridCompareTable from './GridCompareTable'
import { LabelSmall } from 'baseui/typography'
import { createUseStyles } from 'react-jss'
import { val } from './utils'
import { headlineHeight } from './const'

const useStyles = createUseStyles({
    gridComineTable: {
        display: 'flex',
        flexDirection: 'column',
        flex: 1,
        minWidth: 0,
        minHeight: '100%',
    },
    header: { display: 'flex', alignItems: 'center', justifyContent: 'space-between' },
    headerTitle: {
        fontWeight: '600',
        display: 'flex',
        alignItems: 'center',
        fontSize: '14px',
        color: 'rgba(2,16,43,0.60);',
    },
})

const selector = (state: IGridState) => ({
    rowSelectedIds: state.rowSelectedIds,
    rowSelectedRecords: state.rowSelectedRecords,
    setRowSelectedIds: state.setRowSelectedIds,
})

function BaseGridCombineTable({
    // datastore api
    isLoading = false,
    records,
    columnTypes,
    columns,
    // table config
    title = '',
    titleOfCompare = 'Compare',
    queryable = false,
    queryinline = false,
    columnable = false,
    viewable = false,
    previewable = false,
    paginationable = false,
    compareable = true,
    rowHeight,
    // actions
    onSave,
    onChange = () => {},
    emptyMessage,
    emptyColumnMessage = (
        <BusyPlaceholder type='notfound'>Create a new evaluation or Config to add columns</BusyPlaceholder>
    ),
    getId = (record: any) => val(record.id),
}: ITableProps) {
    const styles = useStyles()
    const { rowSelectedIds, setRowSelectedIds, rowSelectedRecords } = useStore(selector)
    const $compareRows = React.useMemo(() => {
        if (rowSelectedIds.length === 0) return []
        if (rowSelectedRecords.length > 0) return rowSelectedRecords
        return records?.filter((r) => rowSelectedIds.includes(getId(r))) ?? []
    }, [rowSelectedIds, rowSelectedRecords, records, getId])

    return (
        <div data-type='grid-combine-table' className={styles.gridComineTable}>
            <ToolBar columnable={columnable} viewable={viewable} queryable={queryable} />
            <GridResizer
                left={() => {
                    return (
                        <MemoGridTable
                            selectable
                            paginationable={paginationable}
                            columns={columns}
                            isLoading={isLoading}
                            onSave={onSave}
                            onChange={onChange}
                            queryinline={queryinline}
                            previewable={previewable}
                            emptyMessage={emptyMessage}
                            emptyColumnMessage={emptyColumnMessage}
                            headlineHeight={headlineHeight}
                            rowHeight={rowHeight}
                            getId={getId}
                        >
                            {title && (
                                <LabelSmall style={{ height: `${headlineHeight}px` }} className={styles.headerTitle}>
                                    {title}
                                </LabelSmall>
                            )}
                        </MemoGridTable>
                    )
                }}
                isResizeable={rowSelectedIds.length > 0 && compareable}
                right={() => (
                    <GridCompareTable
                        rowSelectedIds={rowSelectedIds}
                        onRowSelectedChange={setRowSelectedIds}
                        title={titleOfCompare}
                        records={$compareRows}
                        columnTypes={columnTypes}
                        getId={getId}
                    />
                )}
            />
        </div>
    )
}

export const MemoGridCombineTable = React.memo(BaseGridCombineTable)

export default function GridCombineTable({
    storeKey = 'table-combined',
    initState = {},
    store,
    children,
    ...rest
}: IContextGridTable) {
    return (
        <StoreProvider initState={initState} storeKey={storeKey} store={store}>
            <StoreUpdater {...rest} />
            <MemoGridCombineTable {...rest}>{children}</MemoGridCombineTable>
        </StoreProvider>
    )
}
