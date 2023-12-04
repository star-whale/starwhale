import React, { useEffect } from 'react'
import { ColumnSchemaDesc, useFetchDatastoreByTable } from '@starwhale/core/datastore'
import { ITableState, createCustomStore } from '@starwhale/ui/GridTable/store'
import { shallow } from 'zustand/shallow'
import useDatastorePage from '@starwhale/core/datastore/hooks/useDatastorePage'
import { useDatastoreSummaryColumns } from '@starwhale/ui/GridDatastoreTable/hooks/useDatastoreSummaryColumns'
import GridCombineTable from '@starwhale/ui/GridTable/GridCombineTable'
import { useEventCallback } from '@starwhale/core'
import { IProjectSchema } from '@/domain/project/schemas/project'
import usePrevious from '@starwhale/ui/utils/usePrevious'
import _ from 'lodash'
import { useUnmount } from 'react-use'
import { IProjectVo } from '@/api'

const selector = (s: ITableState) => ({
    rowSelectedIds: s.rowSelectedIds,
    currentView: s.currentView,
    initStore: s.initStore,
    getRawConfigs: s.getRawConfigs,
    onCurrentViewIdChange: s.onCurrentViewIdChange,
    onSelectNone: s.onSelectNone,
    getRawIfChangedConfigs: s.getRawIfChangedConfigs,
    onCurrentViewQueriesChange: s.onCurrentViewQueriesChange,
    reset: s.reset,
})

export const useStore = createCustomStore('add-evaluation', {}, false)

export type EvalSelectDataT = Record<
    string,
    {
        summaryTableName: string
        records: Record<string, any>[]
        columnTypes: ColumnSchemaDesc[]
        rowSelectedIds: string[]
        currentView: ITableState['currentView']
        projectId: string
        project?: IProjectSchema
    }
>

const defaultQueries = [
    {
        op: 'EQUAL',
        property: 'sys/job_status',
        value: 'SUCCESS',
    },
]

function EvalSelectForm({
    project,
    summaryTableName,
    onSelectedDataChange,
}: {
    project?: IProjectVo
    summaryTableName?: string
    onSelectedDataChange: (data: string[]) => void
}) {
    const projectId = project?.id

    const [cachedSelectRecords, setCachedSelectRecords] = React.useState<any[]>([])

    const { currentView, reset, onCurrentViewQueriesChange } = useStore(selector, shallow)

    const { page, setPage, params } = useDatastorePage({
        pageNum: 1,
        pageSize: 999,
        sortBy: currentView?.sortBy || 'sys/id',
        sortDirection: currentView?.sortBy ? (currentView?.sortDirection as any) : 'DESC',
        queries: currentView?.queries,
        tableName: summaryTableName,
    })

    const { columnTypes, records, columnHints } = useFetchDatastoreByTable(params, !!projectId)

    const recordById = React.useMemo(() => {
        return _.keyBy(records, (r) => r.id?.value)
    }, [records])

    const cachedSelectRecordsById = React.useMemo(() => {
        return _.keyBy(cachedSelectRecords, (r) => r.id?.value)
    }, [cachedSelectRecords])

    const $columns = useDatastoreSummaryColumns({
        projectId,
        columnTypes,
        columnHints,
    })

    const handelRowSelectedChange = useEventCallback((ids: any[]) => {
        const rows = ids.map((id) => recordById[id] || cachedSelectRecordsById[id]).filter(Boolean)

        setCachedSelectRecords(rows)

        onSelectedDataChange(ids)
    })

    // init store with initial state
    const prevId = usePrevious(projectId)
    useEffect(() => {
        if (prevId === projectId) return
        onCurrentViewQueriesChange(defaultQueries)
    }, [projectId, onCurrentViewQueriesChange, prevId])

    useUnmount(() => {
        reset()
    })

    return (
        <GridCombineTable
            paginationable
            queryable
            // columnable
            compareable={false}
            page={page}
            onPageChange={setPage}
            store={useStore}
            records={records}
            columnTypes={columnTypes}
            columnHints={columnHints}
            columns={$columns}
            onRowSelectedChange={handelRowSelectedChange}
        />
    )
}

export { EvalSelectForm }
export default EvalSelectForm
