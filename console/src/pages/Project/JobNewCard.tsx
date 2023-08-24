import React, { useCallback } from 'react'
import Card from '@/components/Card'
import useTranslation from '@/hooks/useTranslation'
import JobForm from '@job/components/JobForm'
import { ICreateJobSchema } from '@job/schemas/job'
import { createJob, fetchJob } from '@job/services/job'
import { useParams } from 'react-router-dom'
import { useQuery } from 'react-query'
import { useQueryArgs } from '@starwhale/core/utils'

export default function JobNewCard() {
    const { projectId } = useParams<{ projectId: string }>()
    const { query } = useQueryArgs()
    const [t] = useTranslation()
    const handleSubmit = useCallback(
        async (data: ICreateJobSchema) => {
            if (!projectId) {
                return
            }
            await createJob(projectId, data)
        },
        [projectId]
    )
    // rerun job id
    const { rid } = query
    const info = useQuery(`fetchJobs:${projectId}:${rid}`, () => fetchJob(projectId, rid), {
        refetchOnWindowFocus: false,
        enabled: !!rid,
    })

    return (
        <Card title={t('Run Model')}>
            <JobForm onSubmit={handleSubmit} job={info.data} autoFill={!rid} />
        </Card>
    )
}
