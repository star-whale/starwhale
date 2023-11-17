import React, { useCallback } from 'react'
import Card from '@/components/Card'
import useTranslation from '@/hooks/useTranslation'
import JobForm from '@job/components/JobForm'
import { fetchJob } from '@job/services/job'
import { useHistory, useParams } from 'react-router-dom'
import { useQuery } from 'react-query'
import { useQueryArgs } from '@starwhale/core/utils'
import { IJobRequest, api } from '@/api'
import { ExtendButton } from '@starwhale/ui'

export default function FineTuneNewCard() {
    const { projectId, spaceId } = useParams<{ projectId: any; spaceId: any }>()
    const { query } = useQueryArgs()
    const [t] = useTranslation()
    const history = useHistory()
    const handleSubmit = useCallback(
        async (data: IJobRequest) => {
            if (!projectId) {
                return
            }
            await api.createJob(projectId, {
                ...data,
                type: 'FINE_TUNE',
                bizType: 'FINE_TUNE',
                bizId: spaceId,
            })
            history.push(`/projects/${projectId}/spaces/${spaceId}/fine-tunes`)
        },
        [projectId, spaceId, history]
    )
    // rerun job id
    const { rid } = query
    const info = useQuery(`fetchJobs:${projectId}:${rid}`, () => fetchJob(projectId, rid), {
        refetchOnWindowFocus: false,
        enabled: !!rid,
    })

    return (
        <Card
            title={
                <div className='flex gap-10px font-18px font-800'>
                    <ExtendButton
                        icon='arrow_left'
                        styleas={['iconnormal', 'nopadding']}
                        onClick={() => history.go(-1)}
                    />
                    {t('ft.job.new')}
                </div>
            }
        >
            <JobForm onSubmit={handleSubmit} job={info.data} autoFill={!rid} enableTemplate={false} />
        </Card>
    )
}
