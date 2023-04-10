import { useRuntime, useRuntimeLoading } from '@/domain/runtime/hooks/useRuntime'
import useTranslation from '@/hooks/useTranslation'
import React, { useEffect, useMemo } from 'react'
import { useQuery } from 'react-query'
import { useHistory, useParams } from 'react-router-dom'
import { INavItem } from '@/components/BaseSidebar'
import { fetchRuntime, removeRuntime } from '@/domain/runtime/services/runtime'
import BaseSubLayout from '@/pages/BaseSubLayout'
import { BaseNavTabs } from '@/components/BaseNavTabs'
import RuntimeVersionSelector from '@/domain/runtime/components/RuntimeVersionSelector'
import qs from 'qs'
import { usePage } from '@/hooks/usePage'
import { useRuntimeVersion, useRuntimeVersionLoading } from '@/domain/runtime/hooks/useRuntimeVersion'
import { ConfirmButton } from '@starwhale/ui/Modal'
import { toaster } from 'baseui/toast'
import { Button } from '@starwhale/ui'
import { useRouterActivePath } from '@/hooks/useRouterActivePath'

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

    const runtimeVersionInfo = useQuery(`fetchRuntimeVersion:${projectId}:${runtimeId}:${runtimeVersionId}`, () =>
        fetchRuntime(projectId, runtimeId, runtimeVersionId)
    )
    const { setRuntimeVersion } = useRuntimeVersion()
    const { setRuntimeVersionLoading } = useRuntimeVersionLoading()

    useEffect(() => {
        setRuntimeVersionLoading(runtimeVersionInfo.isLoading)
        if (runtimeVersionInfo.isSuccess) setRuntimeVersion(runtimeVersionInfo.data)
    }, [
        setRuntimeVersionLoading,
        runtimeVersionInfo.isSuccess,
        runtimeVersionInfo.isLoading,
        runtimeVersionInfo.data,
        setRuntimeVersion,
    ])

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
                pattern: '/meta\\/?',
            },
            {
                title: t('Files'),
                path: `/projects/${projectId}/runtimes/${runtimeId}/versions/${runtimeVersionId}/files`,
                pattern: '/files\\/?',
            },
        ]
        return items
    }, [projectId, runtimeId, runtimeVersionId, t])

    const { activeItemId } = useRouterActivePath(navItems)

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
        <BaseSubLayout breadcrumbItems={breadcrumbItems} extra={extra}>
            <div style={{ flex: 1, display: 'flex', flexDirection: 'column' }}>
                {runtimeVersionId && (
                    <div style={{ display: 'flex', justifyContent: 'space-between', marginBottom: '20px' }}>
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
                            kind='secondary'
                            onClick={() => history.push(`/projects/${projectId}/runtimes/${runtimeId}/versions`)}
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
        </BaseSubLayout>
    )
}
