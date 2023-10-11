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
import usePrevious from '@starwhale/ui/utils/usePrevious'
import _ from 'lodash'
import { useUnmount } from 'react-use'

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

function EvalProjectList({
    projectId,
    project,
    onSelectedDataChange,
}: {
    projectId: string
    project?: IProjectSchema
    onSelectedDataChange: (data: EvalSelectDataT) => void
}) {
    const summaryTableName = tableNameOfSummary(projectId)

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

    const $columns = useDatastoreSummaryColumns(columnTypes as any, {
        projectId,
    })

    const handelRowSelectedChange = useEventCallback((ids: any[]) => {
        const rows = ids.map((id) => recordById[id] || cachedSelectRecordsById[id]).filter(Boolean)

        setCachedSelectRecords(rows)

        onSelectedDataChange({
            [projectId]: {
                projectId,
                project,
                summaryTableName,
                records: rows,
                columnTypes,
                columnHints,
            } as any,
        })
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

const EvalSelectForm = React.forwardRef(
    (
        // eslint-disable-next-line
        { initialSelectData }: { initialSelectData?: EvalSelectDataT },
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
                        projectId={projectId}
                        project={projectItem}
                        onSelectedDataChange={(r: any) =>
                            // eslint-disable-next-line
                            setSelectData((prev: any) => {
                                return {
                                    // disable cached other project for now
                                    // ...prev,
                                    ...r,
                                }
                            })
                        }
                    />
                </div>
            </div>
        )
    }
)

export { EvalSelectForm }
export default EvalSelectForm
