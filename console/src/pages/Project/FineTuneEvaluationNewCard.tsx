import React, { useCallback } from 'react'
import Card from '@/components/Card'
import useTranslation from '@/hooks/useTranslation'
import JobForm from '@job/components/JobForm'
import { useHistory, useParams } from 'react-router-dom'
import { useQueryArgs } from '@starwhale/core/utils'
import { IJobRequest, api } from '@/api'
import { ExtendButton } from '@starwhale/ui'
import { useRouteInlineContext } from '@/contexts/RouteInlineContext'

export default function FineTuneEvaluationNewCard() {
    const { projectId, spaceId } = useParams<{ projectId: any; spaceId: any }>()
    const { query } = useQueryArgs()
    const [t] = useTranslation()
    const history = useHistory()
    const { jobId } = query
    const { isInline } = useRouteInlineContext()

    const handleSubmit = useCallback(
        async (data: IJobRequest) => {
            if (!projectId) {
                return
            }
            await api.createJob(projectId, {
                ...data,
                type: 'EVALUATION',
                bizType: 'FINE_TUNE',
                bizId: spaceId,
            })
            if (isInline) {
                history.go(-1)
                return
            }
            history.push(`/projects/${projectId}/spaces/${spaceId}/fine-tune-evals`)
        },
        [projectId, spaceId, history, isInline]
    )
    const info = api.useGetJob(projectId, jobId)
    const job = info?.data

    return (
        <Card
            title={
                <div className='flex gap-10px font-18px font-800'>
                    {!isInline && (
                        <ExtendButton
                            icon='arrow_left'
                            styleas={['iconnormal', 'nopadding']}
                            onClick={() => history.go(-1)}
                        />
                    )}
                    {t('ft.job.new')}
                </div>
            }
        >
            <JobForm onSubmit={handleSubmit} job={job} autoFill={!job} enableTemplate={false} />
        </Card>
    )
}
