import React from 'react'
import useTranslation from '@/hooks/useTranslation'
import { IJobVo } from '@/api'
import useFineTuneColumns from '@/domain/space/hooks/useFineTuneColumns'
import { useEventCallback } from '@starwhale/core'
import { useCreation } from 'ahooks'
import JobStatus from '@/domain/job/components/JobStatus'
import Alias from '@/components/Alias'

function JobCard({ job, onClick, viewId }: { job: IJobVo }) {
    const [t] = useTranslation()
    const isFocus = viewId === String(job.id)
    const ref = React.useRef<HTMLDivElement>(null)

    React.useEffect(() => {
        if (isFocus) {
            ref.current?.scrollIntoView({
                behavior: 'smooth',
                block: 'center',
                inline: 'center',
            })
        }
    }, [isFocus, job.id])

    return (
        <div
            ref={ref}
            className='flex px-16px py-16px border-1 rounded-sm h-110px gap-10px lh-none overflow-hidden flex-shrink-0'
            onClick={onClick}
            role='button'
            tabIndex={0}
            style={{
                border: viewId === String(job.id) ? '1px solid #2B65D9' : '1px solid #CFD7E6',
            }}
        >
            <div className='flex-1 flex flex-col justify-between'>
                <div className='flex justify-between items-center font-600'>
                    {job.modelName}
                    <JobStatus status={job.jobStatus as any} />
                </div>
                <div className='flex-1 items-center mt-12px mb-auto'>
                    <Alias alias={job.model.version.alias} />
                </div>
                <div className='flex justify-between items-center color-[rgba(2,16,43,0.60)]'></div>
            </div>
        </div>
    )
}

export default function FineTuneOnlineEvalJobCard({
    list,
    viewId,
    onView,
}: {
    list?: IJobVo[]
    onView?: (id: number) => void
    viewId?: any
}) {
    const cards = useCreation(() => {
        return list?.map((job) => <JobCard key={job.id} job={job} onClick={() => onView?.(job.id)} viewId={viewId} />)
    }, [list, viewId])

    return <div className='ft-table-card content-full-scroll gap-10px pr-5px'>{cards}</div>
}
