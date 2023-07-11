import React, { useCallback } from 'react'
import Card from '@/components/Card'
import useTranslation from '@/hooks/useTranslation'
import { ICreateJobSchema } from '@job/schemas/job'
import { createJob } from '@job/services/job'
import { useParams } from 'react-router-dom'
import { useQueryArgs } from '@starwhale/core'

const DatasetForm = React.lazy(
    () => import(/* webpackChunkName: "datasetForm" */ '@/domain/dataset/components/DatasetForm')
)

export default function DatasetNewCard() {
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

    return (
        <Card title={query?.datasetVersionId ? t('dataset.update') : t('dataset.create')}>
            <React.Suspense fallback={null}>
                <DatasetForm onSubmit={handleSubmit} />
            </React.Suspense>
        </Card>
    )
}
