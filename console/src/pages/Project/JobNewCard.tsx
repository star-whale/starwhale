import React, { useCallback, useState } from 'react'
import Card from '@/components/Card'
import { usePage } from '@/hooks/usePage'
import useTranslation from '@/hooks/useTranslation'
import { Button, SIZE as ButtonSize } from 'baseui/button'
import JobForm from '@job/components/JobForm'
import { ICreateJobSchema } from '@job/schemas/job'
import { createJob } from '@job/services/job'
import { useParams } from 'react-router'

export default function JobNewCard() {
    const [page] = usePage()
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
            <JobForm onSubmit={handleSubmit}></JobForm>
        </Card>
    )
}
