import React from 'react'
import useTranslation from '@/hooks/useTranslation'
import { IFineTuneVo, api } from '@/api'
import { useHistory, useParams } from 'react-router-dom'
import { usePage } from '@/hooks/usePage'
import FineTuneRunsTable from './FineTuneRunsTable'
import FineTuneRunsTableCard from './FineTuneRunsTableCard'

export default function FineTuneRunsListCard({ isExpand, onView, viewId, data, onRefresh }) {
    const [t] = useTranslation()
    const ref = React.useRef<HTMLDivElement>(null)
    const [page] = usePage()
    const { projectId, spaceId } = useParams<{ projectId: any; spaceId: any }>()

    return (
        <div ref={ref} className='ft-list content-full'>
            {/* <div className='grid gap-20px grid-cols-[280px_1fr_16px_16px] mb-20px'>
                <div className='flex-1'>
                    <Search
                        value={queries}
                        getFilters={(key) => (attrs.find((v) => v.key === key) || attrs[0])?.getFilters()}
                        onChange={setQueries as any}
                    />
                </div>
            </div> */}
            {isExpand ? (
                <FineTuneRunsTableCard data={data} onView={onView} viewId={viewId} />
            ) : (
                <FineTuneRunsTable data={data} onView={onView} viewId={viewId} onRefresh={onRefresh} />
            )}
        </div>
    )
}
