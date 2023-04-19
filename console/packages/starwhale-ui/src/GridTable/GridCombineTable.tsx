import React, { useCallback, useMemo, useState } from 'react'
import Card from '@/components/Card'
import { createJob } from '@job/services/job'
import { ICreateJobSchema } from '@job/schemas/job'
import JobForm from '@job/components/JobForm'
import { durationToStr, formatTimestampDateTime } from '@/utils/datetime'
import useTranslation from '@/hooks/useTranslation'
import { Modal, ModalHeader, ModalBody } from 'baseui/modal'
import { useHistory, useParams, Prompt } from 'react-router-dom'
import { CustomColumn } from '@starwhale/ui/base/data-table'
import { useDrawer } from '@/hooks/useDrawer'
import _ from 'lodash'
import { ITableState, useEvaluationCompareStore, useEvaluationStore } from '@starwhale/ui/base/data-table/store'
import { useFetchViewConfig } from '@/domain/evaluation/hooks/useFetchViewConfig'
import { setEvaluationViewConfig } from '@/domain/evaluation/services/evaluation'
import useFetchDatastoreByTable from '@starwhale/core/datastore/hooks/useFetchDatastoreByTable'
import { tableNameOfSummary } from '@starwhale/core/datastore/utils'
import { TextLink } from '@/components/Link'
import { WithCurrentAuth } from '@/api/WithAuth'
import { GridTable, MemoGridTable } from '@starwhale/ui/GridTable'
import { useDatastoreColumns } from '@starwhale/ui/GridDatastoreTable'
import { toaster } from 'baseui/toast'
import { BusyPlaceholder, Button, GridResizer } from '@starwhale/ui'
import { useLocalStorage } from 'react-use'
import { useProject } from '@project/hooks/useProject'
import JobStatus from '@/domain/job/components/JobStatus'
import { useDatastoreMixedSchema } from '@starwhale/core/datastore'
import { createUseStyles } from 'react-jss'
import ToolBar from '@starwhale/ui/GridTable/components/ToolBar'
import { GridResizerVertical } from '@starwhale/ui/AutoResizer/GridResizerVertical'
import EvaluationListResult from '@/pages/Evaluation/EvaluationListResult'
import EvaluationListCompare from '@/pages/Evaluation/EvaluationListCompare'
import { useStore } from './hooks/useStore'
import { ITableProps, IContextGridTable } from './types'
import { StoreProvider } from './store'

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
    data = [],
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
    const $rows = useMemo(
        () =>
            data.map((raw, index) => {
                return {
                    id: getId(raw) ?? index.toFixed(),
                    data: raw,
                }
            }),
        [data]
    )
    const $compareRows = React.useMemo(() => {
        return records.filter((r) => rowSelectedIds.includes(r.id)) ?? []
    }, [rowSelectedIds, records])

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
                                columns={columns}
                                rows={$rows}
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
                    right={() => {
                        return <EvaluationListCompare title={titleOfCompare} rows={$compareRows} attrs={columnTypes} />
                    }}
                />
            )}
            isResizeable={$compareRows.length > 0}
            bottom={() => <EvaluationListResult title={titleOfDetail} rows={$compareRows} />}
        />
    )
}

export { BaseGridCombineTable }

export default function GridCombineTable({
    storeKey = 'table',
    initState = {},
    store = undefined,
    ...rest
}: IContextGridTable) {
    return (
        <StoreProvider initState={initState} storeKey={storeKey} store={store}>
            <GridCombineTable {...rest} />
        </StoreProvider>
    )
}
