import React, { useCallback } from 'react'
import Card from '@/components/Card'
import useTranslation from '@/hooks/useTranslation'
import { useParams } from 'react-router-dom'
import { useQueryArgs } from '@starwhale/core'
import { createDataset } from '@/domain/dataset/services/dataset'
import { ICreateDatasetFormSchema } from '@/domain/dataset/schemas/dataset'

const DatasetForm = React.lazy(
    () => import(/* webpackChunkName: "datasetForm" */ '@/domain/dataset/components/DatasetForm')
)

export default function DatasetNewCard() {
    const { projectId, datasetId } = useParams<{ projectId: string; datasetId: string }>()
    const { query } = useQueryArgs()
    const [t] = useTranslation()
    const handleSubmit = useCallback(
        async (data: ICreateDatasetFormSchema) => {
            console.log(data)
            if (!projectId || !data.datasetName || !data.storagePath) {
                return
            }

            await createDataset(projectId, data.datasetName, {
                datasetId,
                type: data.type,
                shared: data.shared,
                storagePath: data.storagePath,
            })
        },
        [projectId, datasetId]
    )

    return (
        <Card title={query?.datasetVersionId ? t('dataset.update') : t('dataset.create')}>
            <React.Suspense fallback={null}>
                <DatasetForm onSubmit={handleSubmit} />
            </React.Suspense>
        </Card>
    )
}
