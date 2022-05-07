import { useDataset, useDatasetLoading } from '@dataset/hooks/useDataset'
import useTranslation from '@/hooks/useTranslation'
import React, { useEffect, useMemo } from 'react'
import { useQuery } from 'react-query'
import { useParams } from 'react-router-dom'
import { INavItem } from '@/components/BaseSidebar'
import { fetchDataset } from '@dataset/services/dataset'
import BaseSubLayout from '@/pages/BaseSubLayout'
import { useFetchProject } from '@/domain/project/hooks/useFetchProject'

export interface IDatasetLayoutProps {
    children: React.ReactNode
}

export default function DatasetVersionLayout({ children }: IDatasetLayoutProps) {
    const { projectId, datasetId } = useParams<{ datasetId: string; projectId: string }>()
    const datasetInfo = useQuery(`fetchDataset:${projectId}:${datasetId}`, () => fetchDataset(projectId, datasetId))
    const { dataset, setDataset } = useDataset()
    const projectInfo = useFetchProject(projectId)
    const { setDatasetLoading } = useDatasetLoading()
    useEffect(() => {
        setDatasetLoading(datasetInfo.isLoading)
        if (datasetInfo.isSuccess) {
            if (datasetInfo.data.id !== dataset?.id) {
                setDataset(datasetInfo.data)
            }
        } else if (datasetInfo.isLoading) {
            setDataset(undefined)
        }
    }, [dataset?.id, datasetInfo.data, datasetInfo.isLoading, datasetInfo.isSuccess, setDataset, setDatasetLoading])

    const [t] = useTranslation()
    const datasetName = dataset?.name ?? '-'
    const project = projectInfo.data ?? {}

    const breadcrumbItems: INavItem[] = useMemo(() => {
        const items = [
            {
                title: t('Datasets'),
                path: `/projects/${project?.id}/datasets`,
            },
            {
                title: datasetName,
                path: `/projects/${project?.id}/datasets/${datasetId}`,
            },
            {
                title: t('dataset versions'),
                path: `/projects/${project?.id}/datasets/${datasetId}/versions`,
            },
        ]
        return items
    }, [project?.id, datasetId, datasetName, t])

    return <BaseSubLayout breadcrumbItems={breadcrumbItems}>{children}</BaseSubLayout>
}
