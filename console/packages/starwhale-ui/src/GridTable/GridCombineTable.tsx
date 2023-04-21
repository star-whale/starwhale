import React, { useMemo } from 'react'
import _ from 'lodash'
import { ITableState } from '@starwhale/ui/base/data-table/store'
import { BusyPlaceholder, GridResizer } from '@starwhale/ui'
import ToolBar from '@starwhale/ui/GridTable/components/ToolBar'
import { GridResizerVertical } from '@starwhale/ui/AutoResizer/GridResizerVertical'
import EvaluationListResult from '@/pages/Evaluation/EvaluationListResult'
import EvaluationListCompare from '@/pages/Evaluation/EvaluationListCompare'
import { useStore, useStoreApi } from './hooks/useStore'
import { ITableProps, IContextGridTable } from './types'
import { StoreProvider, StoreUpdater, useDirectStoreUpdater } from './store'
import { MemoGridTable } from './GridTable'
import GridCompareTable, { MemoGridCompareTable } from './GridCompareTable'
import { useDatastoreColumns } from '../GridDatastoreTable'
import store from '@starwhale/core/store/store'
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

const selector = (state: ITableState) => ({
    rowSelectedIds: state.rowSelectedIds,
})

function BaseGridCombineTable({
    // datastore api
    isLoading = false,
    records,
    columnTypes,
    // table confi
    title = '',
    titleOfDetail = 'Detail',
    titleOfCompare = 'Compare',
    paginationProps,
    rowActions,
    searchable = false,
    filterable = false,
    queryable = false,
    compareable = false,
    selectable = false,
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
    storeRef,
    children,
    getId = (record: any) => val(record.id),
}: ITableProps) {
    const styles = useStyles()
    const { rowSelectedIds } = useStore(selector)
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

export { BaseGridCombineTable }

export default function GridCombineTable({
    storeKey = 'table-combined',
    initState = {},
    store = undefined,
    children,
    ...rest
}: IContextGridTable) {
    return (
        <StoreProvider initState={initState} storeKey={storeKey} store={store}>
            <StoreUpdater {...rest}>
                <BaseGridCombineTable {...rest}>{children}</BaseGridCombineTable>
            </StoreUpdater>
        </StoreProvider>
    )
}
