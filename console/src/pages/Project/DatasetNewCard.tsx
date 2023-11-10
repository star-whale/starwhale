import React, { useCallback } from 'react'
import Card from '@/components/Card'
import useTranslation from '@/hooks/useTranslation'
import { useHistory, useParams } from 'react-router-dom'
import { useQueryArgs } from '@starwhale/core'
import { createDataset } from '@/domain/dataset/services/dataset'
import { ICreateDatasetFormSchema } from '@/domain/dataset/schemas/dataset'

const DatasetForm = React.lazy(
    () => import(/* webpackChunkName: "datasetForm" */ '@/domain/dataset/components/DatasetForm')
)

export default function DatasetNewCard() {
    const { projectId } = useParams<{ projectId: string; datasetId: string }>()
    const { query } = useQueryArgs()
    const [t] = useTranslation()
    const history = useHistory()

    const handleSubmit = useCallback(
        async (data: ICreateDatasetFormSchema) => {
            if (!projectId || !data.datasetName || !data.upload?.storagePath || !data.upload?.type) {
                return
            }
            await createDataset(projectId, data.datasetName, {
                type: data.upload?.type as any,
                shared: !!data.shared,
                storagePath: data.upload?.storagePath,
            })
            history.push(`/projects/${projectId}/datasets`)
        },
        [projectId, history]
    )

    return (
        <Card title={query?.datasetVersionId ? t('dataset.update') : t('dataset.create')}>
            <React.Suspense fallback={null}>
                <DatasetForm onSubmit={handleSubmit} />
            </React.Suspense>
        </Card>
    )
}
