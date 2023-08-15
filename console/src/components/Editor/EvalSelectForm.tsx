import React, { MutableRefObject, useEffect, useImperativeHandle } from 'react'
import ProjectSelector from '@/domain/project/components/ProjectSelector'
import { ColumnSchemaDesc, tableNameOfSummary, useFetchDatastoreByTable } from '@starwhale/core/datastore'
import { ITableState, createCustomStore } from '@starwhale/ui/GridTable/store'
import shallow from 'zustand/shallow'
import useDatastorePage from '@starwhale/core/datastore/hooks/useDatastorePage'
import { useDatastoreSummaryColumns } from '@starwhale/ui/GridDatastoreTable/hooks/useDatastoreSummaryColumns'
import GridCombineTable from '@starwhale/ui/GridTable/GridCombineTable'
import { useEventCallback } from '@starwhale/core'
import { IProjectSchema } from '@/domain/project/schemas/project'

const selector = (s: ITableState) => ({
    rowSelectedIds: s.rowSelectedIds,
    currentView: s.currentView,
    initStore: s.initStore,
    getRawConfigs: s.getRawConfigs,
    onCurrentViewIdChange: s.onCurrentViewIdChange,
    onSelectNone: s.onSelectNone,
    getRawIfChangedConfigs: s.getRawIfChangedConfigs,
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

function EvalProjectList({
    initialSelectData,
    projectId,
    project,
    onSelectedDataChange,
    onSelectedDataRemove,
}: {
    initialSelectData: EvalSelectDataT
    projectId: string
    project?: IProjectSchema
    onSelectedDataChange: (data: EvalSelectDataT) => void
    onSelectedDataRemove: (projectId: string) => void
}) {
    const summaryTableName = tableNameOfSummary(projectId)

    const { currentView, initStore } = useStore(selector, shallow)

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
        const rows = ids.map((id) => records.find((r) => r.id?.value === id)).filter(Boolean)
        if (!projectId) return
        if (!rows.length) {
            onSelectedDataRemove(projectId)
            return
        }
        onSelectedDataChange({
            [projectId]: {
                projectId,
                project,
                rowSelectedIds: ids,
                currentView,
                summaryTableName,
                records: rows,
                columnTypes,
            } as any,
        })
    })

    const handleCurrentViewChange = useEventCallback((state: ITableState) => {
        const ids = state.rowSelectedIds
        const rows = ids.map((id) => records.find((r) => r.id?.value === id)).filter(Boolean)
        if (!projectId) return
        if (!rows.length) {
            onSelectedDataRemove(projectId)
            return
        }
        onSelectedDataChange({
            [projectId]: {
                projectId,
                project,
                rowSelectedIds: ids,
                currentView: state.currentView,
                summaryTableName,
                records: rows,
                columnTypes,
            } as any,
        })
    })

    // init store with initial state
    useEffect(() => {
        initStore(initialSelectData[projectId])
    }, [projectId, initStore, initialSelectData])

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
            columns={$columns}
            onRowSelectedChange={handelRowSelectedChange}
            onCurrentViewChange={handleCurrentViewChange}
        />
    )
}

const EvalSelectForm = React.forwardRef(
    (
        { initialSelectData }: { initialSelectData: EvalSelectDataT },
        ref: MutableRefObject<
            | {
                  getData: () => EvalSelectDataT
              }
            | undefined
        >
    ) => {
        const [projectId, setProjectId] = React.useState<string>('')
        const [projectItem, setProjectItem] = React.useState<any>(null)
        const [selectData, setSelectData] = React.useState<EvalSelectDataT>({})

        // console.log('selectdata', selectData)

        useImperativeHandle(
            ref,
            () => ({
                getData() {
                    return selectData
                },
            }),
            [selectData]
        )

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
                        initialSelectData={initialSelectData}
                        projectId={projectId}
                        project={projectItem}
                        onSelectedDataRemove={(pid) => {
                            // console.log('remove', pid)
                            // @ts-ignore
                            setSelectData((prev) => {
                                return {
                                    ...prev,
                                    // do not delete, {} used to truncate final list data
                                    [pid]: undefined,
                                }
                            })
                        }}
                        onSelectedDataChange={(r: any) =>
                            setSelectData((prev: any) => ({
                                ...prev,
                                ...r,
                            }))
                        }
                    />
                </div>
            </div>
        )
    }
)

export { EvalSelectForm }
export default EvalSelectForm
