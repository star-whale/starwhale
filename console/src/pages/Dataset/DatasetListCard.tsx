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

export default function DatasetListCard() {
    const [page] = usePage()
    const { projectId } = useParams<{ datasetId: string; projectId: string }>()
    const history = useHistory()

    const datasetsInfo = useFetchDatasets(projectId, page)
    const [t] = useTranslation()

    return (
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
                            <Button
                                key='version-history'
                                kind='tertiary'
                                onClick={() => history.push(`/projects/${projectId}/datasets/${dataset.id}/versions`)}
                            >
                                {t('Version History')}
                            </Button>,
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
    )
}
