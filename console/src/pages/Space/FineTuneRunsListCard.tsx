import React from 'react'
import FineTuneRunsTable from './FineTuneRunsTable'
import FineTuneRunsTableCard from './FineTuneRunsTableCard'
import useFineTuneColumns from '@/domain/space/hooks/useFineTuneColumns'
import Search from '@starwhale/ui/Search'

function FineTuneSearchBar({ getFilters, queries, setQueries }) {
    return <Search value={queries} getFilters={getFilters} onChange={setQueries} />
}

function FineTuneRunsListCard({ isExpand, onView, viewId, data, onRefresh, params }) {
    const ref = React.useRef<HTMLDivElement>(null)

    const { list, getFilters, queries, setQueries } = useFineTuneColumns({ data })

    return (
        <div ref={ref} className='ft-list content-full'>
            <div className={`${isExpand ? 'w-full' : 'w-360px'} mb-20px`}>
                <FineTuneSearchBar getFilters={getFilters} queries={queries} setQueries={setQueries} />
            </div>
            {isExpand ? (
                <FineTuneRunsTableCard list={list} onView={onView} viewId={viewId} />
            ) : (
                <FineTuneRunsTable list={list} onView={onView} onRefresh={onRefresh} params={params} />
            )}
        </div>
    )
}

export { FineTuneRunsListCard, FineTuneSearchBar }
export default FineTuneRunsListCard
