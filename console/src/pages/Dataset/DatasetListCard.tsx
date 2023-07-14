import React from 'react'
import Card from '@/components/Card'
import { usePage } from '@/hooks/usePage'
import { formatTimestampDateTime } from '@/utils/datetime'
import useTranslation from '@/hooks/useTranslation'
import Table from '@/components/Table'
import { useHistory, useParams } from 'react-router-dom'
import { useFetchDatasets } from '@dataset/hooks/useFetchDatasets'
import { TextLink } from '@/components/Link'
import { Button } from '@starwhale/ui'
import Alias from '@/components/Alias'
import { MonoText } from '@/components/Text'
import { WithCurrentAuth } from '@/api/WithAuth'
import User from '@/domain/user/components/User'
import { useQuery } from 'react-query'
import { fetchDatasetBuildList } from '@/domain/dataset/services/dataset'
import qs from 'qs'

export default function DatasetListCard() {
    const [page] = usePage()
    const { projectId } = useParams<{ datasetId: string; projectId: string }>()
    const history = useHistory()

    const datasetsInfo = useFetchDatasets(projectId, page)
    const [t] = useTranslation()

    const query = { status: 'BUILDING', ...page }

    const datasetBuildList = useQuery(`fetchDatasetBuildList:${projectId}:${qs.stringify(query)}`, () =>
        fetchDatasetBuildList(projectId, query)
    )

    console.log(datasetBuildList.data)

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
                <Table
                    isLoading={datasetsInfo.isLoading}
                    columns={[
                        t('sth name', [t('Dataset')]),
                        t('Version'),
                        t('Alias'),
                        t('Owner'),
                        t('Created'),
                        t('Action'),
                    ]}
                    data={
                        datasetsInfo.data?.list.map((dataset) => {
                            return [
                                <TextLink
                                    key={dataset.id}
                                    to={`/projects/${projectId}/datasets/${dataset.id}/versions/${dataset.version?.id}/files`}
                                >
                                    {dataset.name}
                                </TextLink>,
                                <MonoText key='name'>{dataset.version?.name ?? '-'}</MonoText>,
                                <Alias key='alias' alias={dataset.version?.alias} />,
                                dataset.owner && <User user={dataset.owner} />,
                                dataset.createdTime && formatTimestampDateTime(dataset.createdTime),
                                <div key='version-history' style={{ display: 'flex', gap: '5px' }}>
                                    <Button
                                        as='link'
                                        kind='tertiary'
                                        onClick={() =>
                                            history.push(`/projects/${projectId}/datasets/${dataset.id}/versions`)
                                        }
                                    >
                                        {t('Version History')}
                                    </Button>
                                    <Button
                                        as='link'
                                        icon='upload'
                                        onClick={() =>
                                            history.push(
                                                `/projects/${projectId}/new_dataset/${dataset.id}?datasetName=${dataset.name}`
                                            )
                                        }
                                    />
                                </div>,
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
                }}
            >
                <Button as='link' onClick={() => history.push(`/projects/${projectId}/datasets/builds`)}>
                    build list{' '}
                </Button>
            </div>
        </>
    )
}
