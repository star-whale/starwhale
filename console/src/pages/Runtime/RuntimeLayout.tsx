import { useRuntime, useRuntimeLoading } from '@/domain/runtime/hooks/useRuntime'
import useTranslation from '@/hooks/useTranslation'
import React, { useEffect, useMemo } from 'react'
import { useQuery } from 'react-query'
import { useParams } from 'react-router-dom'
import { INavItem } from '@/components/BaseSidebar'
import { fetchRuntime } from '@/domain/runtime/services/runtime'
import BaseSubLayout from '@/pages/BaseSubLayout'
import { formatTimestampDateTime } from '@/utils/datetime'
import Accordion from '@/components/Accordion'
import { Panel } from 'baseui/accordion'

export interface IRuntimeLayoutProps {
    children: React.ReactNode
}

export default function RuntimeLayout({ children }: IRuntimeLayoutProps) {
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

    const breadcrumbItems: INavItem[] = useMemo(() => {
        const items = [
            {
                title: t('Runtimes'),
                path: `/projects/${projectId}/runtimes`,
            },
            {
                title: runtime?.versionMeta ?? '-',
                path: `/projects/${projectId}/runtimes/${runtimeId}`,
            },
        ]
        return items
    }, [projectId, runtimeId, t, runtime])

    const header = React.useMemo(() => {
        const items = [
            {
                label: t('Runtime Name'),
                value: runtime?.name ?? '',
            },
            {
                label: t('Version Name'),
                value: runtime?.versionName ?? '',
            },
            {
                label: t('Version Tag'),
                value: runtime?.versionTag ?? '',
            },
            {
                label: t('Created'),
                value: runtime?.createdTime && formatTimestampDateTime(runtime.createdTime),
            },
        ]

        const info = (
            <div
                style={{
                    fontSize: '14px',
                    display: 'grid',
                    gridTemplateColumns: 'repeat(auto-fit, minmax(420px, 1fr))',
                    gap: '12px',
                }}
            >
                {items.map((v) => (
                    <div key={v?.label} style={{ display: 'flex', gap: '12px' }}>
                        <div
                            style={{
                                lineHeight: '24px',
                                borderRadius: '4px',
                                color: 'rgba(2,16,43,0.60)',
                            }}
                        >
                            {v?.label}:
                        </div>
                        <div> {v?.value}</div>
                    </div>
                ))}
            </div>
        )
        return (
            <div className='mb-20'>
                <Accordion accordion>
                    <Panel title={`${t('Runtime ID')}: ${runtime?.id ?? ''}`}>{info}</Panel>
                </Accordion>
            </div>
        )
    }, [runtime, t])

    return (
        <BaseSubLayout breadcrumbItems={breadcrumbItems} header={header}>
            {children}
        </BaseSubLayout>
    )
}
