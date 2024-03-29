import { useDataset, useDatasetLoading } from '@dataset/hooks/useDataset'
import useTranslation from '@/hooks/useTranslation'
import React, { useEffect, useMemo } from 'react'
import { useQuery } from 'react-query'
import { useParams } from 'react-router-dom'
import { INavItem } from '@/components/BaseSidebar'
import { fetchDataset } from '@dataset/services/dataset'
import BaseSubLayout from '@/pages/BaseSubLayout'

export interface IDatasetLayoutProps {
    children: React.ReactNode
}

export default function DatasetVersionLayout({ children }: IDatasetLayoutProps) {
    const { projectId, datasetId } = useParams<{ datasetId: string; projectId: string }>()
    const datasetInfo = useQuery(`fetchDataset:${projectId}:${datasetId}`, () => fetchDataset(projectId, datasetId))
    const { dataset, setDataset } = useDataset()
    const { setDatasetLoading } = useDatasetLoading()
    useEffect(() => {
        setDatasetLoading(datasetInfo.isLoading)
        if (datasetInfo.isSuccess) {
            if (datasetInfo.data.versionName !== dataset?.versionName) {
                setDataset(datasetInfo.data)
            }
        } else if (datasetInfo.isLoading) {
            setDataset(undefined)
        }
    }, [
        dataset?.versionName,
        datasetInfo.data,
        datasetInfo.isLoading,
        datasetInfo.isSuccess,
        setDataset,
        setDatasetLoading,
    ])

    const [t] = useTranslation()
    const datasetName = dataset?.versionName ?? '-'
    const breadcrumbItems: INavItem[] = useMemo(() => {
        const items = [
            {
                title: t('Datasets'),
                path: `/projects/${projectId}/datasets`,
            },
            {
                title: datasetName,
                path: `/projects/${projectId}/datasets/${datasetId}`,
            },
            {
                title: t('dataset versions'),
                path: `/projects/${projectId}/datasets/${datasetId}/versions`,
            },
        ]
        return items
    }, [projectId, datasetId, datasetName, t])

    return <BaseSubLayout breadcrumbItems={breadcrumbItems}>{children}</BaseSubLayout>
}
