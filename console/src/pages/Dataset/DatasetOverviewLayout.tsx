import { useDataset, useDatasetLoading } from '@dataset/hooks/useDataset'
import useTranslation from '@/hooks/useTranslation'
import React, { useEffect, useMemo } from 'react'
import { useHistory, useParams } from 'react-router-dom'
import { INavItem } from '@/components/BaseSidebar'
import BaseSubLayout from '@/pages/BaseSubLayout'
import DatasetVersionSelector from '@/domain/dataset/components/DatasetVersionSelector'
import { BaseNavTabs } from '@/components/BaseNavTabs'
import { useFetchDatasetVersion } from '@/domain/dataset/hooks/useFetchDatasetVersion'
import { useFetchDataset } from '@/domain/dataset/hooks/useFetchDataset'
import { useDatasetVersion } from '@/domain/dataset/hooks/useDatasetVersion'
import qs from 'qs'
import { usePage } from '@/hooks/usePage'
import Search from '@starwhale/ui/Search'
import { RecordListSchemaT, useQueryDatasetList } from '@starwhale/core/datastore'
import { useQueryArgs } from '@/hooks/useQueryArgs'
import { Button } from '@starwhale/ui'
import { ConfirmButton } from '@starwhale/ui/Modal'
import { removeDataset } from '@/domain/dataset/services/dataset'
import { toaster } from 'baseui/toast'
import { useRouterActivePath } from '@/hooks/useRouterActivePath'
import { DatastoreMixedTypeRecordSearch } from '@starwhale/ui/Search/Search'

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
    const { columnInfo: datastore } = useQueryDatasetList(datasetVersion?.indexTable, params, true)

    const breadcrumbItems: INavItem[] = useMemo(() => {
        const items = [
            {
                title: t('Datasets'),
                path: `/projects/${projectId}/datasets`,
            },
            {
                title: dataset?.name ?? '-',
                path: `/projects/${projectId}/datasets/${datasetId}`,
            },
        ]
        return items
    }, [datasetId, projectId, dataset, t])

    const pageParams = useMemo(() => {
        return { ...page, ...query }
    }, [page, query])

    const navItems: INavItem[] = useMemo(() => {
        const items = [
            {
                title: t('Overview'),
                path: `/projects/${projectId}/datasets/${datasetId}/versions/${datasetVersionId}/overview?${qs.stringify(
                    pageParams
                )}`,
                pattern: '/\\/overview\\/?',
            },
            {
                title: t('Metadata'),
                path: `/projects/${projectId}/datasets/${datasetId}/versions/${datasetVersionId}/meta?${qs.stringify(
                    pageParams
                )}`,
                pattern: '/\\/meta\\/?',
            },
            {
                title: t('Files'),
                path: `/projects/${projectId}/datasets/${datasetId}/versions/${datasetVersionId}/files?${qs.stringify(
                    pageParams
                )}`,
                pattern: '/\\/files\\/?',
            },
        ]
        return items
    }, [projectId, datasetId, datasetVersionId, t, pageParams])

    const { activeItemId } = useRouterActivePath(navItems)

    const extra = useMemo(() => {
        return (
            <ConfirmButton
                as='negative'
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
        <BaseSubLayout breadcrumbItems={breadcrumbItems} extra={extra}>
            <div style={{ flex: 1, display: 'flex', flexDirection: 'column' }}>
                {datasetVersionId && (
                    <div style={{ display: 'flex', justifyContent: 'space-between', marginBottom: '20px' }}>
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
                        <Button
                            icon='runtime'
                            kind='secondary'
                            onClick={() => history.push(`/projects/${projectId}/datasets/${datasetId}`)}
                        >
                            {t('History')}
                        </Button>
                    </div>
                )}
                {datasetVersionId && (
                    <div style={{ marginBottom: '10px' }}>
                        <DatastoreMixedTypeRecordSearch
                            records={datastore.data?.records as RecordListSchemaT}
                            value={query.filter ? query.filter.filter((v: any) => v.value) : undefined}
                            onChange={(items) => {
                                updateQuery({ filter: items.filter((v) => v.value) as any })
                            }}
                        />
                    </div>
                )}
                {datasetVersionId && (
                    <div style={{ marginBottom: '20px' }}>
                        <BaseNavTabs navItems={navItems} />
                    </div>
                )}
                <div style={{ flex: '1', display: 'flex', flexDirection: 'column' }}>{children}</div>
            </div>
        </BaseSubLayout>
    )
}
