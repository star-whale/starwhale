import React from 'react'
import EvaluationListCard from './Evaluation/EvaluationListCard'
import { api } from '@/api'
import RouteOverview from './RouteOverview'
import FineTuneJobActionGroup from '@/domain/space/components/FineTuneJobActionGroup'
import Search from '@starwhale/ui/Search'
import FineTuneRunsTableCard from './FineTuneRunsTableCard'
import { useFetchDatastoreByTable } from '@starwhale/core/datastore'
import useDatastorePage from '@starwhale/core/datastore/hooks/useDatastorePage'
import { useDatastoreColumns } from '@starwhale/ui/GridDatastoreTable'
import { DatastoreMixedTypeSearch } from '@starwhale/ui/Search/Search'
import useFineTuneEvaluation from '@/domain/space/hooks/useFineTuneEvaluation'

function FineTuneSearchBar({ getFilters, queries, setQueries }) {
    return <Search value={queries} getFilters={getFilters} onChange={setQueries} />
}

export default function FineTuneEvaluationListCard() {
    const config = useFineTuneEvaluation()
    const { projectId, spaceId, gotoList, gotoDetails, jobId, routes } = config

    // const { renderCell } = useFineTuneColumns()
    const info = api.useListFineTune(projectId, spaceId)
    const onRefresh = () => info.refetch()

    const isExpand = !!jobId
    const url = isExpand && routes.evaluationOverview

    // const title = useCreation(() => {
    //     if (!fineTune) return null
    //     const renderer = renderCell(fineTune)
    //     return (
    //         <>
    //             <div className='flex items-center font-600'>{renderer('baseModelName')}</div>
    //             <div className='flex-1 items-center mt-6px mb-auto'>{renderer('baseModelVersionAlias')}</div>
    //         </>
    //     )
    // }, [fineTuneId, fineTune])

    const params = {
        projectId,
        spaceId,
    }

    const ref = React.useRef<HTMLDivElement>(null)

    // const { list, getFilters, queries, setQueries } = useFineTuneColumns({ data: info.data })

    const [queries, setQueries] = React.useState([])
    const { getQueryParams } = useDatastorePage({
        pageNum: 1,
        pageSize: 1000,
        queries,
    })
    const datatore = useFetchDatastoreByTable(getQueryParams(config.summaryTableName))
    const $columns = useDatastoreColumns(datatore)

    return (
        <div className={`grid gap-15px content-full ${isExpand ? 'grid-cols-[360px_1fr]' : 'grid-cols-1'}`}>
            <div ref={ref} className='ft-list content-full'>
                {isExpand ? (
                    <>
                        <div className='w-full mb-20px'>
                            {/* <FineTuneSearchBar getFilters={getFilters} queries={queries} setQueries={setQueries} /> */}
                            <DatastoreMixedTypeSearch columns={$columns} value={queries} onChange={setQueries as any} />
                        </div>
                        <FineTuneRunsTableCard list={datatore.records} onView={gotoDetails} viewId={jobId} />
                    </>
                ) : (
                    <EvaluationListCard {...config} />
                )}
            </div>
            {isExpand && (
                <RouteOverview
                    title={null}
                    url={url}
                    onClose={gotoList}
                    extraActions={<FineTuneJobActionGroup onRefresh={onRefresh} {...params} />}
                />
            )}
        </div>
    )
}
