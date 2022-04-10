import { useModel, useModelLoading } from '@model/hooks/useModel'
import useTranslation from '@/hooks/useTranslation'
import React, { useEffect, useMemo } from 'react'
import { useQuery } from 'react-query'
import { useParams } from 'react-router-dom'
import { INavItem } from '@/components/BaseSidebar'
import { fetchModel } from '@model/services/model'
import BaseSubLayout from '@/pages/BaseSubLayout'
import { useFetchProject } from '@/domain/project/hooks/useFetchProject'

export interface IModelLayoutProps {
    children: React.ReactNode
}

export default function ModelLayout({ children }: IModelLayoutProps) {
    const { projectId, modelId } = useParams<{ modelId: string; projectId: string }>()
    const modelInfo = useQuery(`fetchModel:${projectId}:${modelId}`, () => fetchModel(projectId, modelId))
    const { model, setModel } = useModel()
    const projectInfo = useFetchProject(projectId)
    const { setModelLoading } = useModelLoading()
    useEffect(() => {
        setModelLoading(modelInfo.isLoading)
        if (modelInfo.isSuccess) {
            if (modelInfo.data.id !== model?.id) {
                setModel(modelInfo.data)
            }
        } else if (modelInfo.isLoading) {
            setModel(undefined)
        }
    }, [model?.id, modelInfo.data, modelInfo.isLoading, modelInfo.isSuccess, setModel, setModelLoading])

    const [t] = useTranslation()
    const modelName = model?.name ?? '-'
    const project = projectInfo.data ?? {}
    const projectName = project?.name ?? '-'

    const breadcrumbItems: INavItem[] = useMemo(() => {
        const items = [
            // {
            //     title: t('projects'),
            //     path: '/projects',
            // },
            // {
            //     title: project?.name ?? '-',
            //     path: `/projects/${project?.id}`,
            // },
            {
                title: t('Models'),
                path: `/projects/${project?.id}/models`,
            },
            {
                title: modelName,
                path: `/projects/${project?.id}/models/${modelId}`,
            },
        ]
        return items
    }, [projectName, modelName, t])

    // const navItems: INavItem[] = useMemo(
    //     () => [
    //         // {
    //         //     title: modelName ?? t('overview'),
    //         //     path: `/models/${modelId}`,
    //         //     icon: RiSurveyLine,
    //         // },
    //     ],
    //     [modelName, t]
    // )
    return <BaseSubLayout breadcrumbItems={breadcrumbItems}>{children}</BaseSubLayout>
}
