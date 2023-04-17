import React from 'react'
import Card from '@/components/Card'
import { usePage } from '@/hooks/usePage'
import { formatTimestampDateTime } from '@/utils/datetime'
import useTranslation from '@/hooks/useTranslation'
import Table from '@/components/Table'
import { useHistory, useParams } from 'react-router-dom'
import { useFetchRuntimes } from '@/domain/runtime/hooks/useFetchRuntimes'
import User from '@/domain/user/components/User'
import { TextLink } from '@/components/Link'
import { Button, ConfirmButton } from '@starwhale/ui'
import Alias from '@/components/Alias'
import { MonoText } from '@/components/Text'
import { buildImageForRuntimeVersion } from '@runtime/services/runtimeVersion'
import { toaster } from 'baseui/toast'

export default function RuntimeListCard() {
    const [page] = usePage()
    const history = useHistory()
    const { projectId } = useParams<{ runtimeId: string; projectId: string }>()

    const runtimesInfo = useFetchRuntimes(projectId, page)

    const [t] = useTranslation()

    return (
        <Card title={t('Runtimes')}>
            <Table
                isLoading={runtimesInfo.isLoading}
                columns={[
                    t('sth name', [t('Runtime')]),
                    t('Runtime Version'),
                    t('Alias'),
                    t('Image'),
                    t('Owner'),
                    t('Created'),
                    t('Action'),
                ]}
                data={
                    runtimesInfo.data?.list.map((runtime) => {
                        return [
                            <TextLink
                                key={runtime.id}
                                to={`/projects/${projectId}/runtimes/${runtime.id}/versions/${runtime.version?.id}/meta`}
                            >
                                {runtime.name}
                            </TextLink>,
                            <MonoText key='name'>{runtime.version?.name ?? '-'}</MonoText>,
                            <Alias key='alias' alias={runtime.version?.alias} />,
                            runtime.version?.image ?? '-',
                            runtime.owner && <User user={runtime.owner} />,
                            runtime.createdTime && formatTimestampDateTime(runtime.createdTime),
                            <>
                                <Button
                                    key='version-history'
                                    kind='tertiary'
                                    onClick={() => history.push(`/projects/${projectId}/runtimes/${runtime.id}`)}
                                >
                                    {t('Version History')}
                                </Button>
                                &nbsp;&nbsp;
                                {runtime.version?.builtImage ?? (
                                    <ConfirmButton
                                        as='transparent'
                                        title={t('runtime.image.build.confirm')}
                                        onClick={async () => {
                                            const result = await buildImageForRuntimeVersion(
                                                projectId,
                                                runtime.id,
                                                runtime.version.id
                                            )
                                            if (result.success) {
                                                toaster.positive(result.message, {
                                                    autoHideDuration: 1000,
                                                })
                                            } else {
                                                toaster.negative(result.message, {
                                                    autoHideDuration: 2000,
                                                })
                                            }
                                        }}
                                    >
                                        {t('runtime.image.build.button')}
                                    </ConfirmButton>
                                )}
                            </>,
                        ]
                    }) ?? []
                }
                paginationProps={{
                    start: runtimesInfo.data?.pageNum,
                    count: runtimesInfo.data?.pageSize,
                    total: runtimesInfo.data?.total,
                    afterPageChange: () => {
                        runtimesInfo.refetch()
                    },
                }}
            />
        </Card>
    )
}
