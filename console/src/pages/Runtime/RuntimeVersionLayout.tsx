import { useRuntime, useRuntimeLoading } from '@/domain/runtime/hooks/useRuntime'
import useTranslation from '@/hooks/useTranslation'
import React, { useEffect, useMemo } from 'react'
import { useQuery } from 'react-query'
import { useParams } from 'react-router-dom'
import { INavItem } from '@/components/BaseSidebar'
import { fetchRuntime } from '@/domain/runtime/services/runtime'
import BaseSubLayout from '@/pages/BaseSubLayout'
import { useFetchProject } from '@/domain/project/hooks/useFetchProject'

export interface IRuntimeLayoutProps {
    children: React.ReactNode
}

export default function RuntimeVersionLayout({ children }: IRuntimeLayoutProps) {
    const { projectId, runtimeId } = useParams<{ runtimeId: string; projectId: string }>()
    const runtimeInfo = useQuery(`fetchRuntime:${projectId}:${runtimeId}`, () => fetchRuntime(projectId, runtimeId))
    const { runtime, setRuntime } = useRuntime()
    const projectInfo = useFetchProject(projectId)
    const { setRuntimeLoading } = useRuntimeLoading()
    useEffect(() => {
        setRuntimeLoading(runtimeInfo.isLoading)
        if (runtimeInfo.isSuccess) {
            if (runtimeInfo.data.versionMeta !== runtime?.versionMeta) {
                setRuntime(runtimeInfo.data)
            }
        } else if (runtimeInfo.isLoading) {
            setRuntime(undefined)
        }
    }, [
        runtime?.versionMeta,
        runtimeInfo.data,
        runtimeInfo.isLoading,
        runtimeInfo.isSuccess,
        setRuntime,
        setRuntimeLoading,
    ])

    const [t] = useTranslation()
    const runtimeName = runtime?.versionMeta ?? '-'
    const project = projectInfo.data ?? {}

    const breadcrumbItems: INavItem[] = useMemo(() => {
        const items = [
            {
                title: t('Runtimes'),
                path: `/projects/${project?.id}/runtimes`,
            },
            {
                title: runtimeName,
                path: `/projects/${project?.id}/runtimes/${runtimeId}`,
            },
            {
                title: t('runtime versions'),
                path: `/projects/${project?.id}/runtimes/${runtimeId}/versions`,
            },
        ]
        return items
    }, [runtimeName, t, project?.id, runtimeId])

    return <BaseSubLayout breadcrumbItems={breadcrumbItems}>{children}</BaseSubLayout>
}
