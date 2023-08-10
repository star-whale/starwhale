import React, { useImperativeHandle } from 'react'
import { GridTable } from '@starwhale/ui/GridTable'
import ProjectSelector from '@/domain/project/components/ProjectSelector'
import { ColumnSchemaDesc, tableNameOfSummary, useFetchDatastoreByTable } from '@starwhale/core/datastore'
import { ITableState, createCustomStore } from '@starwhale/ui/GridTable/store'
import shallow from 'zustand/shallow'
import useDatastorePage from '@starwhale/core/datastore/hooks/useDatastorePage'
import { useDatastoreSummaryColumns } from '@starwhale/ui/GridDatastoreTable/hooks/useDatastoreSummaryColumns'
import GridCombineTable from '@starwhale/ui/GridTable/GridCombineTable'
import { useEvent } from 'react-use'
import { useEventCallback } from '@starwhale/core'

const selector = (s: ITableState) => ({
    rowSelectedIds: s.rowSelectedIds,
    currentView: s.currentView,
    initStore: s.initStore,
    getRawConfigs: s.getRawConfigs,
    onCurrentViewIdChange: s.onCurrentViewIdChange,
    getRawIfChangedConfigs: s.getRawIfChangedConfigs,
})

export const useStore = createCustomStore('add-evaluation', {}, false)

function EvalProjectList({
    projectId,
    onRowSelectedChange,
}: {
    projectId: string
    onRowSelectedChange: (
        data: Record<
            string,
            {
                records: Record<string, any>[]
                columnTypes: ColumnSchemaDesc[]
            }
        >
    ) => void
}) {
    const summaryTableName = tableNameOfSummary(projectId)

    const { currentView } = useStore(selector, shallow)

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

    const handelRowSelectedChange = useEventCallback((ids: any[]) => {
        const rows = ids.map((id) => records.find((r) => r.id?.value === id)) as any

        onRowSelectedChange({
            [summaryTableName]: {
                records: rows,
                columnTypes,
            },
        })
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
            onRowSelectedChange={handelRowSelectedChange}
        />
    )
}

const EvalSelectForm = React.forwardRef((props, ref: any) => {
    const [projectId, setProjectId] = React.useState<string>('')
    const [projectItem, setProjectItem] = React.useState<any>(null)
    const [rows, setRows] = React.useState<
        Record<
            string,
            {
                records: Record<string, any>[]
                columnTypes: ColumnSchemaDesc[]
            }
        >
    >({})

    useImperativeHandle(
        ref,
        () => ({
            getData() {
                return { projectId, projectItem, tableMap: rows }
            },
        }),
        [projectId, projectItem, rows]
    )

    console.log(rows)

    return (
        <div className='flex flex-column gap-12px'>
            <div className='w-280px'>
                <ProjectSelector
                    value={projectId}
                    onChange={(id) => setProjectId(id)}
                    onChangeItem={(item) => setProjectItem(item)}
                />
            </div>
            <div className='h-380px w-full'>
                <EvalProjectList
                    projectId={projectId}
                    onRowSelectedChange={(r: any) =>
                        setRows((prev: any) => ({
                            ...prev,
                            ...r,
                        }))
                    }
                />
            </div>
        </div>
    )
})

export { EvalSelectForm }
export default EvalSelectForm
