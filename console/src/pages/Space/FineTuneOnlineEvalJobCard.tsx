import React from 'react'
import useTranslation from '@/hooks/useTranslation'
import { IJobVo } from '@/api'
import { useCreation } from 'ahooks'
import JobStatus from '@/domain/job/components/JobStatus'
import Alias from '@/components/Alias'
import { useJobActions } from '@/domain/job/components/JobActionGroup'
import { useProject } from '@/domain/project/hooks/useProject'
import { ExtendButton } from '@starwhale/ui/Button'
import { useHistory } from 'react-router-dom'
import { useChatStore } from '@starwhale/ui/Serving/store/chat'
import { useServingConfig } from '@starwhale/ui/Serving/store/config'

function JobCard({ job }: { job: IJobVo }) {
    const [t] = useTranslation()
    const history = useHistory()
    const ref = React.useRef<HTMLDivElement>(null)
    const { project } = useProject() as any
    const { getActions } = useJobActions()
    const cancel = getActions({ job, project })?.find((v) => v.key === 'cancel')
    const Cancel: React.FC<any> = cancel ? cancel.component : () => null
    const chatStore = useChatStore()
    const session = chatStore.getSessionById(job.id)
    const disabled = !session?.serving
    const config = useServingConfig()

    return (
        <div
            ref={ref}
            className='flex px-16px py-16px border-1 rounded-sm h-110px gap-10px lh-none overflow-hidden flex-shrink-0'
            role='button'
            tabIndex={0}
            style={{
                border: '1px solid #CFD7E6',
            }}
        >
            <div className='py-1px'>
                <ExtendButton
                    disabled={disabled}
                    icon={session?.show ? 'eye' : 'eye_off'}
                    styleas={['menuoption', 'nopadding', 'iconnormal', !session?.serving ? 'icondisable' : undefined]}
                    onClick={() => session && chatStore.onSessionShowById(session.id, !session?.show)}
                />
            </div>
            <div className='flex-1 flex flex-col justify-between'>
                <div className='flex justify-between items-center font-600'>
                    {job.modelName}
                    <JobStatus status={job.jobStatus as any} />
                </div>
                <div className='flex-1 items-center my-12px mb-auto'>
                    <Alias alias={job.model.version.alias} />
                </div>
                <div className='flex justify-end color-[rgba(2,16,43,0.60)] gap-20px'>
                    {!disabled && (
                        <ExtendButton as='link' onClick={() => chatStore.onSessionEditParamsShow(session?.id)}>
                            {t('ft.online_eval.parameter.setting')}
                        </ExtendButton>
                    )}
                    <span className='w-1px bg-[#EEF1F6]' />
                    <ExtendButton
                        as='link'
                        onClick={() => history.push(`/projects/${project?.id}/jobs/${job?.id}/tasks`)}
                    >
                        {t('View Tasks')}
                    </ExtendButton>
                    {cancel && (
                        <>
                            <span className='w-1px bg-[#EEF1F6]' />
                            <div className='flex justify-end flex-shrink-0'>
                                <Cancel
                                    hasText
                                    hasIcon={false}
                                    styleas={['nopadding', 'negative']}
                                    onDone={() => {
                                        config.refetch()
                                    }}
                                />
                            </div>
                        </>
                    )}
                </div>
            </div>
        </div>
    )
}

export default function FineTuneOnlineEvalJobCard({ list }: { list?: IJobVo[] }) {
    const cards = useCreation(() => {
        return list?.map((job) => <JobCard key={job.id} job={job} />)
    }, [list])

    return <div className='chat-job-card content-full-scroll gap-10px pr-5px'>{cards}</div>
}
