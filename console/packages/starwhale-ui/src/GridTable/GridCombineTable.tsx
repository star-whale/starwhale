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
import { StoreProvider, StoreUpdater } from './store'
import { MemoGridTable } from './GridTable'
import { MemoGridCompareTable } from './GridCompareTable'
import { useDatastoreColumns } from '../GridDatastoreTable'

const selector = (state: ITableState) => ({
    rowSelectedIds: state.rowSelectedIds,
})
function BaseGridCombineTable({
    // datastore api
    isLoading = false,
    records,
    columnTypes,
    // api rendered for table
    columns = [],
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
    getId = (record: any) => record.id,
    storeRef,
    onColumnsChange,
    children,
}: ITableProps) {
    const { rowSelectedIds } = useStore(selector)
    const $compareRows = React.useMemo(() => {
        return records?.filter((r) => rowSelectedIds.includes(r.id)) ?? []
    }, [rowSelectedIds, records])
    const $columns = useDatastoreColumns(columnTypes as any)

    console.log($columns)

    return (
        <GridResizerVertical
            top={() => (
                <GridResizer
                    left={() => {
                        return (
                            <MemoGridTable
                                queryable
                                selectable
                                isLoading={isLoading}
                                columns={$columns}
                                data={records}
                                onSave={onSave}
                                onChange={onChange}
                                emptyColumnMessage={emptyColumnMessage}
                            >
                                <ToolBar columnable={columnable} viewable={viewable} />
                            </MemoGridTable>
                        )
                    }}
                    isResizeable={$compareRows.length > 0}
                    right={() => (
                        <MemoGridCompareTable title={titleOfCompare} rows={$compareRows} columnTypes={columnTypes} />
                    )}
                />
            )}
            isResizeable={$compareRows.length > 0}
            bottom={() => <EvaluationListResult title={titleOfDetail} rows={$compareRows} />}
        />
    )
}

export { BaseGridCombineTable }

export default function GridCombineTable({
    storeKey = 'table-combined',
    initState = {},
    store = undefined,
    ...rest
}: IContextGridTable) {
    return (
        <StoreProvider initState={initState} storeKey={storeKey} store={store}>
            <BaseGridCombineTable {...rest} />
            <StoreUpdater {...rest} />
        </StoreProvider>
    )
}
