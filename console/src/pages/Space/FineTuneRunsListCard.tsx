import React from 'react'
import useTranslation from '@/hooks/useTranslation'
import { IFineTuneVo, api } from '@/api'
import { useHistory, useParams } from 'react-router-dom'
import { usePage } from '@/hooks/usePage'
import FineTuneRunsTable from './FineTuneRunsTable'

export default function FineTuneRunsListCard() {
    const [t] = useTranslation()
    const [expandId, setExpandId] = React.useState<number | undefined>(undefined)
    const [current, setCurrent] = React.useState<string | undefined>(undefined)
    const ref = React.useRef<HTMLDivElement>(null)
    const [page] = usePage()
    const history = useHistory()
    const { projectId, spaceId } = useParams<{ projectId: any; spaceId: any }>()
    const info = api.useListFineTune(projectId, spaceId, {
        ...page,
    })
    const sources = info?.data?.list ?? []

    return (
        <div ref={ref} className='task-event-list overflow-hidden h-full'>
            {/* <div className='grid gap-20px grid-cols-[280px_1fr_16px_16px] mb-20px'>
                <div className='flex-1'>
                    <Search
                        value={queries}
                        getFilters={(key) => (attrs.find((v) => v.key === key) || attrs[0])?.getFilters()}
                        onChange={setQueries as any}
                    />
                </div>
            </div> */}
            <FineTuneRunsTable data={info?.data} />
        </div>
    )
}
