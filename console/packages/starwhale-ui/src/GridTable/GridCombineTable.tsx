import React from 'react'
import { BusyPlaceholder, GridResizer } from '@starwhale/ui'
import ToolBar from '@starwhale/ui/GridTable/components/ToolBar'
import { useStore } from './hooks/useStore'
import { ITableProps, IContextGridTable, IGridState } from './types'
import { StoreProvider, StoreUpdater } from './store'
import { MemoGridTable } from './GridTable'
import GridCompareTable from './GridCompareTable'
import { LabelSmall } from 'baseui/typography'
import { createUseStyles } from 'react-jss'
import { val } from './utils'

const useStyles = createUseStyles({
    gridComineTable: {
        display: 'flex',
        flexDirection: 'column',
        flex: 1,
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
    setRowSelectedIds: state.setRowSelectedIds,
})

function BaseGridCombineTable({
    // datastore api
    isLoading = false,
    records,
    columnTypes,
    columns,
    // table confi
    title = '',
    titleOfCompare = 'Compare',
    queryable = false,
    queryinline = false,
    columnable = false,
    viewable = false,
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
    const { rowSelectedIds, setRowSelectedIds } = useStore(selector)
    const $compareRows = React.useMemo(() => {
        return records?.filter((r) => rowSelectedIds.includes(getId(r))) ?? []
    }, [rowSelectedIds, records, getId])

    return (
        <div data-type='grid-combine-table' className={styles.gridComineTable}>
            <ToolBar columnable={columnable} viewable={viewable} queryable={queryable} />
            <GridResizer
                left={() => {
                    return (
                        <MemoGridTable
                            queryable
                            selectable
                            columns={columns}
                            isLoading={isLoading}
                            onSave={onSave}
                            onChange={onChange}
                            queryinline={queryinline}
                            emptyMessage={emptyMessage}
                            emptyColumnMessage={emptyColumnMessage}
                        >
                            {title && (
                                <LabelSmall style={{ height: '52px' }} className={styles.headerTitle}>
                                    {title}
                                </LabelSmall>
                            )}
                        </MemoGridTable>
                    )
                }}
                isResizeable={rowSelectedIds.length > 0}
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
