import { useRuntime, useRuntimeLoading } from '@/domain/runtime/hooks/useRuntime'
import useTranslation from '@/hooks/useTranslation'
import React, { useEffect, useMemo } from 'react'
import { useQuery } from 'react-query'
import { useParams } from 'react-router-dom'
import { INavItem } from '@/components/BaseSidebar'
import { fetchRuntime } from '@/domain/runtime/services/runtime'
import BaseSubLayout from '@/pages/BaseSubLayout'

export interface IRuntimeLayoutProps {
    children: React.ReactNode
}

export default function RuntimeVersionLayout({ children }: IRuntimeLayoutProps) {
    const { projectId, runtimeId } = useParams<{ runtimeId: string; projectId: string }>()
    const runtimeInfo = useQuery(`fetchRuntime:${projectId}:${runtimeId}`, () => fetchRuntime(projectId, runtimeId))
    const { runtime, setRuntime } = useRuntime()
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

    const breadcrumbItems: INavItem[] = useMemo(() => {
        const items = [
            {
                title: t('Runtimes'),
                path: `/projects/${projectId}/runtimes`,
            },
            {
                title: runtimeName,
                path: `/projects/${projectId}/runtimes/${runtimeId}`,
            },
            {
                title: t('runtime versions'),
                path: `/projects/${projectId}/runtimes/${runtimeId}/versions`,
            },
        ]
        return items
    }, [runtimeName, t, projectId, runtimeId])

    return <BaseSubLayout breadcrumbItems={breadcrumbItems}>{children}</BaseSubLayout>
}
