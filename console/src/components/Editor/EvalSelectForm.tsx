import React from 'react'
import { GridTable } from '@starwhale/ui/GridTable'
import ProjectSelector from '@/domain/project/components/ProjectSelector'
import { tableNameOfSummary, useFetchDatastoreByTable } from '@starwhale/core/datastore'
import { ITableState, createCustomStore } from '@starwhale/ui/GridTable/store'
import shallow from 'zustand/shallow'
import useDatastorePage from '@starwhale/core/datastore/hooks/useDatastorePage'
import { useDatastoreSummaryColumns } from '@starwhale/ui/GridDatastoreTable/hooks/useDatastoreSummaryColumns'
import GridCombineTable from '@starwhale/ui/GridTable/GridCombineTable'

const selector = (s: ITableState) => ({
    rowSelectedIds: s.rowSelectedIds,
    currentView: s.currentView,
    initStore: s.initStore,
    getRawConfigs: s.getRawConfigs,
    onCurrentViewIdChange: s.onCurrentViewIdChange,
    getRawIfChangedConfigs: s.getRawIfChangedConfigs,
})

export const useStore = createCustomStore('add-evaluation', {}, false)

function EvalProjectList({ projectId }) {
    const summaryTableName = tableNameOfSummary(projectId)

    const { rowSelectedIds, currentView } = useStore(selector, shallow)

    const { page, setPage, params } = useDatastorePage({
        pageNum: 1,
        pageSize: 999,
        sortBy: currentView?.sortBy || 'sys/id',
        sortDirection: currentView?.sortBy ? (currentView?.sortDirection as any) : 'DESC',
        queries: currentView?.queries,
        tableName: summaryTableName,
    })
    const { columnTypes, records } = useFetchDatastoreByTable(params, !!projectId)

    const $columns = useDatastoreSummaryColumns(columnTypes as any, {
        projectId,
    })

    return (
        <GridCombineTable
            paginationable
            queryable
            columnable
            compareable={false}
            page={page}
            onPageChange={setPage}
            store={useStore}
            records={records}
            columnTypes={columnTypes}
            columns={$columns}
        />
    )
}

function EvalSelectForm() {
    const [projectId, setProjectId] = React.useState<string>('')

    return (
        <div className='flex flex-column gap-12px'>
            <div className='w-280px'>
                <ProjectSelector value={projectId} onChange={(id) => setProjectId(id)} />
            </div>
            <div className='h-380px w-full'>
                <EvalProjectList projectId={projectId} />
            </div>
        </div>
    )
}
export { EvalSelectForm }
export default EvalSelectForm
