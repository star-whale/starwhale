import React from 'react'
import Card from '@/components/Card'
import { usePage } from '@/hooks/usePage'
import { formatTimestampDateTime } from '@/utils/datetime'
import useTranslation from '@/hooks/useTranslation'
import Table from '@/components/Table'
import { useHistory, useParams } from 'react-router-dom'
import { useFetchDatasets } from '@dataset/hooks/useFetchDatasets'
import { Button, ConfirmButton, ExtendButton, IconFont } from '@starwhale/ui'
import Alias from '@/components/Alias'
import { WithCurrentAuth, useAccess, useAuthPrivileged } from '@/api/WithAuth'
import User from '@/domain/user/components/User'
import { useQuery } from 'react-query'
import { fetchDatasetBuildList, removeDataset } from '@/domain/dataset/services/dataset'
import qs from 'qs'
import Text, { VersionText } from '@starwhale/ui/Text'
import { useProjectRole } from '@/domain/project/hooks/useProjectRole'
import { getAliasStr } from '@base/utils/alias'
import { toaster } from 'baseui/toast'
import yaml from 'js-yaml'
import Shared from '@/components/Shared'
import { QueryInput } from '@starwhale/ui/Input'
import _ from 'lodash'
import { IDatasetSchema } from '@/domain/dataset/schemas/dataset'

export default function DatasetListCard() {
    const [page] = usePage()
    const { projectId } = useParams<{ datasetId: string; projectId: string }>()
    const history = useHistory()
    const [name, setName] = React.useState('')

    const datasetsInfo = useFetchDatasets(projectId, {
        ...page,
        name,
    })
    const [t] = useTranslation()

    const query = { status: 'BUILDING', ...page }

    const { isPrivileged } = useAuthPrivileged({ role: useProjectRole().role, id: 'dataset.create.read' })

    const datasetBuildList = useQuery(
        `fetchDatasetBuildList:${projectId}:${qs.stringify(query)}`,
        () => fetchDatasetBuildList(projectId, query as any),
        {
            enabled: isPrivileged,
        }
    )
    const buildCount = datasetBuildList.data?.list?.length ?? 0

    const isAccessDatasetUpload = useAccess('dataset.upload')
    const isAccessDatasetDelete = useAccess('dataset.delete')

    const getActions = (data: IDatasetSchema) => [
        {
            access: true,
            quickAccess: true,
            component: ({ hasText }) => (
                <ExtendButton
                    isFull
                    icon='overview'
                    tooltip={!hasText ? t('View Details') : undefined}
                    styleas={['menuoption', hasText ? undefined : 'highlight']}
                    onClick={() =>
                        history.push(`/projects/${projectId}/datasets/${data.id}/versions/${data.version?.id}/files`)
                    }
                >
                    {hasText ? t('View Details') : null}
                </ExtendButton>
            ),
        },
        {
            access: true,
            component: ({ hasText }) => (
                <ExtendButton
                    isFull
                    icon='a-Versionhistory'
                    styleas={['menuoption', hasText ? undefined : 'highlight']}
                    onClick={() => history.push(`/projects/${projectId}/datasets/${data.id}/versions`)}
                >
                    {hasText ? t('Version History') : null}
                </ExtendButton>
            ),
        },
        {
            access: isAccessDatasetUpload,
            component: ({ hasText }) => (
                <ExtendButton
                    isFull
                    icon='upload'
                    styleas={['menuoption', hasText ? undefined : 'highlight']}
                    onClick={() =>
                        history.push(
                            `/projects/${projectId}/new_dataset/${data.id}?datasetName=${data.name}&datasetVersion=${data.version?.name}`
                        )
                    }
                >
                    {hasText ? t('Upload') : null}
                </ExtendButton>
            ),
        },
        {
            access: isAccessDatasetDelete,
            component: ({ hasText }) => (
                <ConfirmButton
                    title={`${data.name} ${t('dataset.remove.confirm')}`}
                    isFull
                    icon='delete'
                    styleas={['menuoption', 'negative']}
                    onClick={async () => {
                        await removeDataset(projectId, data.id)
                        toaster.positive(t('dataset.remove.success'), {
                            autoHideDuration: 1000,
                        })
                        datasetsInfo.refetch()
                    }}
                >
                    {hasText ? t('dataset.remove.button') : null}
                </ConfirmButton>
            ),
        },
    ]

    return (
        <>
            <Card
                title={t('Datasets')}
                style={{
                    flexShrink: 1,
                    marginBottom: 0,
                    width: '100%',
                    flex: 1,
                }}
                bodyStyle={{
                    flexDirection: 'column',
                }}
                extra={
                    <WithCurrentAuth id='dataset.create'>
                        <Button onClick={() => history.push('new_dataset')}>{t('create')}</Button>
                    </WithCurrentAuth>
                }
            >
                <div className='max-w-280px mb-10px'>
                    <QueryInput
                        placeholder={t('dataset.search.name.placeholder')}
                        onChange={_.debounce((val: string) => {
                            setName(val.trim())
                        }, 100)}
                    />
                </div>
                <Table
                    renderActions={(rowIndex) => {
                        const data = datasetsInfo.data?.list[rowIndex]
                        if (!data) return undefined
                        return getActions(data)
                    }}
                    isLoading={datasetsInfo.isLoading}
                    columns={[
                        t('sth name', [t('Dataset')]),
                        t('latest.version'),
                        t('latest.version.alias'),
                        t('Shared'),
                        t('dataset.file.count'),
                        t('Owner'),
                        t('Created'),
                    ]}
                    data={
                        datasetsInfo.data?.list.map((dataset) => {
                            let counts
                            try {
                                const meta = yaml.load(dataset.version?.meta || '') as any
                                counts = meta?.dataset_summary?.rows
                                // eslint-disable-next-line no-empty
                            } catch (e) {}

                            return [
                                dataset.name,
                                <VersionText key='name' version={dataset.version?.name ?? '-'} />,
                                dataset.version ? <Alias key='alias' alias={getAliasStr(dataset.version)} /> : null,
                                <Shared key='shared' shared={dataset.version?.shared} isTextShow />,
                                counts,
                                dataset.owner && <User user={dataset.owner} />,
                                dataset.createdTime && formatTimestampDateTime(dataset.createdTime),
                            ]
                        }) ?? []
                    }
                    paginationProps={{
                        start: datasetsInfo.data?.pageNum,
                        count: datasetsInfo.data?.pageSize,
                        total: datasetsInfo.data?.total,
                        afterPageChange: () => {
                            datasetsInfo.refetch()
                        },
                    }}
                />
            </Card>
            <WithCurrentAuth id='dataset.create.read'>
                <div
                    className='dataset-build-list flex'
                    style={{
                        position: 'absolute',
                        bottom: 60,
                        right: '-20px',
                        width: '100px',
                        height: '58px',
                        borderRadius: '100px 0 0 100px',
                        boxShadow: '0 4px 14px 0 rgba(0,0,0,0.30)',
                        backgroundColor: '#fff',
                        zIndex: 2,
                        justifyContent: 'center',
                        alignItems: 'center',
                        gap: '20px',
                    }}
                >
                    <Button as='link' onClick={() => history.push(`/projects/${projectId}/datasets/builds`)}>
                        {buildCount > 0 && (
                            <Text
                                tooltip={t('dataset.create.build.desc', [buildCount])}
                                style={{
                                    marginRight: '20px',
                                }}
                            >
                                {buildCount}
                            </Text>
                        )}
                        <IconFont type='unfold21' style={{ color: 'rgba(2,16,43,0.40)' }} />
                    </Button>
                </div>
            </WithCurrentAuth>
        </>
    )
}
