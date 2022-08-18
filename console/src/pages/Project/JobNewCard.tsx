import React, { useCallback } from 'react'
import Card from '@/components/Card'
import useTranslation from '@/hooks/useTranslation'
import JobForm from '@job/components/JobForm'
import { ICreateJobSchema } from '@job/schemas/job'
import { createJob } from '@job/services/job'
import { useParams } from 'react-router-dom'

export default function JobNewCard() {
    const { projectId } = useParams<{ projectId: string }>()
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

    return (
        <Card title={t('Run Model')}>
            <JobForm onSubmit={handleSubmit} />
        </Card>
    )
}
