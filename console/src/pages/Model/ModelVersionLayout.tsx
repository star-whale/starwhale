import { useModel, useModelLoading } from '@model/hooks/useModel'
import useTranslation from '@/hooks/useTranslation'
import React, { useEffect, useMemo } from 'react'
import { useQuery } from 'react-query'
import { useParams } from 'react-router-dom'
import { INavItem } from '@/components/BaseSidebar'
import { fetchModel } from '@model/services/model'
import BaseSubLayout from '@/pages/BaseSubLayout'

export interface IModelLayoutProps {
    children: React.ReactNode
}

export default function ModelVersionLayout({ children }: IModelLayoutProps) {
    const { projectId, modelId } = useParams<{ modelId: string; projectId: string }>()
    const modelInfo = useQuery(`fetchModel:${projectId}:${modelId}`, () => fetchModel(projectId, modelId))
    const { model, setModel } = useModel()
    const { setModelLoading } = useModelLoading()
    useEffect(() => {
        setModelLoading(modelInfo.isLoading)
        if (modelInfo.isSuccess) {
            if (modelInfo.data.versionInfo.name !== model?.versionName) {
                setModel(modelInfo.data)
            }
        } else if (modelInfo.isLoading) {
            setModel(undefined)
        }
    }, [model?.versionName, modelInfo.data, modelInfo.isLoading, modelInfo.isSuccess, setModel, setModelLoading])

    const [t] = useTranslation()
    const modelName = model?.versionName ?? '-'

    const breadcrumbItems: INavItem[] = useMemo(() => {
        return [
            {
                title: t('Models'),
                path: `/projects/${projectId}/models`,
            },
            {
                title: modelName,
                path: `/projects/${projectId}/models/${modelId}`,
            },
            {
                title: t('model versions'),
                path: `/projects/${projectId}/models/${modelId}/versions`,
            },
        ]
    }, [modelName, t, projectId, modelId])

    return <BaseSubLayout breadcrumbItems={breadcrumbItems}>{children}</BaseSubLayout>
}
