import { useDataset, useDatasetLoading } from '@dataset/hooks/useDataset'
import useTranslation from '@/hooks/useTranslation'
import React, { useEffect, useMemo, useState } from 'react'
import { useHistory, useLocation, useParams } from 'react-router-dom'
import { INavItem } from '@/components/BaseSidebar'
import BaseSubLayout from '@/pages/BaseSubLayout'
import { formatTimestampDateTime } from '@/utils/datetime'
import Accordion from '@/components/Accordion'
import DatasetVersionSelector from '@/domain/dataset/components/DatasetVersionSelector'
import { BaseNavTabs } from '@/components/BaseNavTabs'
import { useFetchDatasetVersion } from '@/domain/dataset/hooks/useFetchDatasetVersion'
import { useFetchDataset } from '@/domain/dataset/hooks/useFetchDataset'
import IconFont from '@/components/IconFont'
import { Panel } from 'baseui/accordion'
import { useDatasetVersion } from '@/domain/dataset/hooks/useDatasetVersion'
import qs from 'qs'
import { usePage } from '@/hooks/usePage'
import Search from '@starwhale/ui/Search'
import { useQueryDatasetList } from '@starwhale/core/datastore'
import { useQueryArgs } from '@/hooks/useQueryArgs'
import { Button } from '@starwhale/ui'
import { ConfirmButton } from '@/components/Modal/confirm'
import { removeDataset } from '@/domain/dataset/services/dataset'
import { toaster } from 'baseui/toast'

export interface IDatasetLayoutProps {
    children: React.ReactNode
}

export default function DatasetOverviewLayout({ children }: IDatasetLayoutProps) {
    const { projectId, datasetId, datasetVersionId } = useParams<{
        datasetId: string
        projectId: string
        datasetVersionId: string
    }>()
    const datasetInfo = useFetchDataset(projectId, datasetId)
    const datasetVersionInfo = useFetchDatasetVersion(projectId, datasetId, datasetVersionId)
    const { dataset, setDataset } = useDataset()
    const { datasetVersion, setDatasetVersion } = useDatasetVersion()
    const { query, updateQuery } = useQueryArgs()
    const [page] = usePage()
    const { setDatasetLoading } = useDatasetLoading()
    const history = useHistory()
    const [t] = useTranslation()

    useEffect(() => {
        setDatasetLoading(datasetInfo.isLoading)
        if (datasetInfo.isSuccess) {
            if (datasetInfo.data?.versionName !== dataset?.versionName) {
                setDataset(datasetInfo.data)
            }
        } else if (datasetInfo.isLoading) {
            setDataset(undefined)
        }
    }, [dataset?.versionName, datasetInfo, setDataset, setDatasetLoading])

    useEffect(() => {
        if (datasetVersionInfo.data) {
            setDatasetVersion(datasetVersionInfo.data)
        }
    }, [datasetVersionInfo.data, setDatasetVersion])

    const params = useMemo(() => {
        return { pageNum: 1, pageSize: 1 }
    }, [])
    const datastore = useQueryDatasetList(datasetVersion?.indexTable, params, true)

    const breadcrumbItems: INavItem[] = useMemo(() => {
        const items = [
            {
                title: t('Datasets'),
                path: `/projects/${projectId}/datasets`,
            },
            {
                title: dataset?.versionName ?? '-',
                path: `/projects/${projectId}/datasets/${datasetId}`,
            },
        ]
        return items
    }, [datasetId, projectId, dataset, t])

    const info = React.useMemo(() => {
        const items = [
            {
                label: t('Version Name'),
                value: dataset?.versionName ?? '',
            },
            {
                label: t('Version Tag'),
                value: dataset?.versionTag ?? '',
            },
            {
                label: t('Created'),
                value: dataset?.createdTime && formatTimestampDateTime(dataset.createdTime),
            },
        ]
        return (
            <div
                style={{
                    fontSize: '14px',
                    display: 'grid',
                    gridTemplateColumns: 'repeat(auto-fit, minmax(380px, 1fr))',
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
    }, [dataset, t])

    const header = useMemo(
        () => (
            <div className='mb-20'>
                <Accordion accordion>
                    <Panel title={t('Overview')}>{info}</Panel>
                </Accordion>
            </div>
        ),
        [info, t]
    )

    const pageParams = useMemo(() => {
        return { ...page, ...query }
    }, [page, query])

    const navItems: INavItem[] = useMemo(() => {
        const items = [
            {
                title: t('Overview'),
                path: `/projects/${projectId}/datasets/${datasetId}/versions/${datasetVersionId}/overview/?${qs.stringify(
                    pageParams
                )}`,
                pattern: '/\\/overview\\/?',
            },
            {
                title: t('Metadata'),
                path: `/projects/${projectId}/datasets/${datasetId}/versions/${datasetVersionId}/meta/?${qs.stringify(
                    pageParams
                )}`,
                pattern: '/\\/meta\\/?',
            },
            {
                title: t('Files'),
                path: `/projects/${projectId}/datasets/${datasetId}/versions/${datasetVersionId}/files/?${qs.stringify(
                    pageParams
                )}`,
                pattern: '/\\/files\\/?',
            },
        ]
        return items
    }, [projectId, datasetId, datasetVersionId, t, pageParams])

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
                title={t('dataset.remove.confirm')}
                onClick={async () => {
                    await removeDataset(projectId, datasetId)
                    toaster.positive(t('dataset.remove.success'), { autoHideDuration: 1000 })
                    history.push(`/projects/${projectId}/datasets`)
                }}
            >
                {t('dataset.remove.button')}
            </ConfirmButton>
        )
    }, [projectId, datasetId, history, t])

    return (
        <BaseSubLayout header={header} breadcrumbItems={breadcrumbItems} extra={extra}>
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
                        <div style={{ display: 'flex', justifyContent: 'space-between', marginBottom: '20px' }}>
                            {datasetVersionId && (
                                <div style={{ width: '300px' }}>
                                    <DatasetVersionSelector
                                        projectId={projectId}
                                        datasetId={datasetId}
                                        value={datasetVersionId}
                                        onChange={(v) =>
                                            history.push(
                                                `/projects/${projectId}/datasets/${datasetId}/versions/${v}/${activeItemId}?${qs.stringify(
                                                    pageParams
                                                )}`
                                            )
                                        }
                                    />
                                </div>
                            )}
                            {datasetVersionId && (
                                <Button
                                    // as='withIcon'
                                    kind='primary'
                                    startEnhancer={() => <IconFont type='runtime' />}
                                    onClick={() => history.push(`/projects/${projectId}/datasets/${datasetId}`)}
                                >
                                    {t('History')}
                                </Button>
                            )}
                        </div>
                        {datasetVersionId && (
                            <Search
                                fields={datastore.data?.columnTypes ?? []}
                                value={query.filter ? query.filter.filter((v: any) => v.value) : undefined}
                                onChange={(items) => {
                                    updateQuery({ filter: items.filter((v) => v.value) as any })
                                }}
                            />
                        )}
                        {datasetVersionId && <BaseNavTabs navItems={navItems} />}
                        <div style={{ paddingTop: '12px', flex: '1', display: 'flex', flexDirection: 'column' }}>
                            {children}
                        </div>
                    </div>
                </Panel>
            </Accordion>
        </BaseSubLayout>
    )
}
