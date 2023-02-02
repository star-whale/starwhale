import { useRuntime, useRuntimeLoading } from '@/domain/runtime/hooks/useRuntime'
import useTranslation from '@/hooks/useTranslation'
import React, { useEffect, useMemo } from 'react'
import { useQuery } from 'react-query'
import { useHistory, useParams, useLocation } from 'react-router-dom'
import { INavItem } from '@/components/BaseSidebar'
import { fetchRuntime, removeRuntime } from '@/domain/runtime/services/runtime'
import BaseSubLayout from '@/pages/BaseSubLayout'
import { formatTimestampDateTime } from '@/utils/datetime'
import Accordion from '@/components/Accordion'
import { Panel } from 'baseui/accordion'
import { BaseNavTabs } from '@/components/BaseNavTabs'
import RuntimeVersionSelector from '@/domain/runtime/components/RuntimeVersionSelector'
import qs from 'qs'
import { usePage } from '@/hooks/usePage'
import { useRuntimeVersion, useRuntimeVersionLoading } from '@/domain/runtime/hooks/useRuntimeVersion'
import { ConfirmButton } from '@/components/Modal/confirm'
import { toaster } from 'baseui/toast'
import { Button } from '@starwhale/ui'

export interface IRuntimeLayoutProps {
    children: React.ReactNode
}

export default function RuntimeOverviewLayout({ children }: IRuntimeLayoutProps) {
    const { projectId, runtimeId, runtimeVersionId } = useParams<{
        runtimeId: string
        projectId: string
        runtimeVersionId: string
    }>()
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

    const runtimeVersionInfo = useQuery(`fetchRuntime:${projectId}:${runtimeId}:${runtimeVersionId}`, () =>
        fetchRuntime(projectId, runtimeId, runtimeVersionId)
    )
    const { setRuntimeVersion } = useRuntimeVersion()
    const { setRuntimeVersionLoading } = useRuntimeVersionLoading()
    useEffect(() => {
        setRuntimeVersionLoading(runtimeVersionInfo.isLoading)
        setRuntimeVersion(runtimeVersionInfo.data)
    }, [setRuntimeVersionLoading, runtimeVersionInfo, setRuntimeVersion])

    const history = useHistory()
    const [page] = usePage()
    const [t] = useTranslation()

    const breadcrumbItems: INavItem[] = useMemo(() => {
        const items = [
            {
                title: t('Runtimes'),
                path: `/projects/${projectId}/runtimes`,
            },
            {
                title: runtime?.name ?? '-',
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

    const navItems: INavItem[] = useMemo(() => {
        const items = [
            {
                title: t('Overview'),
                path: `/projects/${projectId}/runtimes/${runtimeId}/versions/${runtimeVersionId}/overview`,
                pattern: '/\\/overview\\/?',
            },
            {
                title: t('Metadata'),
                path: `/projects/${projectId}/runtimes/${runtimeId}/versions/${runtimeVersionId}/meta`,
                pattern: '/\\/meta\\/?',
            },
            {
                title: t('Files'),
                path: `/projects/${projectId}/runtimes/${runtimeId}/versions/${runtimeVersionId}/files`,
                pattern: '/\\/files\\/?',
            },
        ]
        return items
    }, [projectId, runtimeId, runtimeVersionId, t])

    const location = useLocation()
    const activeItemId = useMemo(() => {
        const item = navItems
            .slice()
            .reverse()
            .find((item_) => (location.pathname ?? '').startsWith(item_.path ?? ''))
        const paths = item?.path?.split('/') ?? []
        return paths[paths.length - 1] ?? 'files'
    }, [location.pathname, navItems])

    const extra = useMemo(() => {
        return (
            <ConfirmButton
                as='negative'
                title={t('runtime.remove.confirm')}
                onClick={async () => {
                    await removeRuntime(projectId, runtimeId)
                    toaster.positive(t('runtime.remove.success'), { autoHideDuration: 1000 })
                    history.push(`/projects/${projectId}/runtimes`)
                }}
            >
                {t('runtime.remove.button')}
            </ConfirmButton>
        )
    }, [projectId, runtimeId, history, t])

    return (
        <BaseSubLayout breadcrumbItems={breadcrumbItems} header={header} extra={extra}>
            <Accordion
                accordion
                overrides={{
                    ToggleIcon: () => <></>,
                    ContentAnimationContainer: {
                        style: {
                            flex: 1,
                            display: 'flex',
                            flexDirection: 'column',
                        },
                    },
                }}
            >
                <Panel title={t('Version and Files')} expanded>
                    <div style={{ flex: 1, display: 'flex', flexDirection: 'column' }}>
                        {runtimeVersionId && (
                            <div style={{ display: 'flex', justifyContent: 'space-between', marginBottom: '10px' }}>
                                <div style={{ width: '280px' }}>
                                    <RuntimeVersionSelector
                                        projectId={projectId}
                                        runtimeId={runtimeId}
                                        value={runtimeVersionId}
                                        onChange={(v: string) =>
                                            history.push(
                                                `/projects/${projectId}/runtimes/${runtimeId}/versions/${v}/${activeItemId}?${qs.stringify(
                                                    page
                                                )}`
                                            )
                                        }
                                    />
                                </div>
                                <Button
                                    icon='runtime'
                                    kind='tertiary'
                                    onClick={() =>
                                        history.push(`/projects/${projectId}/runtimes/${runtimeId}/versions`)
                                    }
                                >
                                    {t('History')}
                                </Button>
                            </div>
                        )}
                        {runtimeVersionId && (
                            <div style={{ marginBottom: '10px' }}>
                                <BaseNavTabs navItems={navItems} />{' '}
                            </div>
                        )}
                        <div style={{ flex: '1', display: 'flex', flexDirection: 'column' }}>{children}</div>
                    </div>
                </Panel>
            </Accordion>
        </BaseSubLayout>
    )
}
