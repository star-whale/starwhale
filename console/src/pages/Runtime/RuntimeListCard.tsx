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
import { ButtonGroup, ConfirmButton, ExtendButton } from '@starwhale/ui'
import { VersionText } from '@starwhale/ui/Text'
import Alias from '@/components/Alias'
import { buildImageForRuntimeVersion } from '@runtime/services/runtimeVersion'
import { toaster } from 'baseui/toast'
import { WithCurrentAuth } from '@/api/WithAuth'
import { getAliasStr } from '@base/utils/alias'
import { removeRuntime } from '@/domain/runtime/services/runtime'
import Shared from '@/components/Shared'

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
                    t('Shared'),
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
                            <VersionText key='name' version={runtime.version?.name ?? '-'} />,
                            runtime.version && <Alias key='alias' alias={getAliasStr(runtime.version)} />,
                            <Shared key='shared' shared={runtime.version?.shared} isTextShow />,
                            runtime.version?.image ?? '-',
                            runtime.owner && <User user={runtime.owner} />,
                            runtime.version?.createdTime && formatTimestampDateTime(runtime.version?.createdTime),
                            <ButtonGroup key='action'>
                                <ExtendButton
                                    as='link'
                                    icon='a-Versionhistory'
                                    tooltip={t('Version History')}
                                    onClick={() => history.push(`/projects/${projectId}/runtimes/${runtime.id}`)}
                                />
                                <WithCurrentAuth id='runtime.image.build'>
                                    <ExtendButton
                                        disabled={!!runtime.version?.builtImage}
                                        iconDisable={!!runtime.version?.builtImage}
                                        as='link'
                                        icon={runtime.version?.builtImage ? 'a-ImageBuilt' : 'a-BuildImage'}
                                        tooltip={
                                            runtime.version?.builtImage
                                                ? t('runtime.image.built')
                                                : t('runtime.image.build')
                                        }
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
                                    />
                                </WithCurrentAuth>
                                <WithCurrentAuth id='runtime.delete'>
                                    <ConfirmButton
                                        tooltip={t('runtime.remove.button')}
                                        title={t('runtime.remove.confirm')}
                                        as='link'
                                        negative
                                        icon='delete'
                                        onClick={async () => {
                                            await removeRuntime(projectId, runtime.id)
                                            toaster.positive(t('runtime.remove.success'), { autoHideDuration: 1000 })
                                            history.push(`/projects/${projectId}/runtimes`)
                                        }}
                                    />
                                </WithCurrentAuth>
                            </ButtonGroup>,
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
