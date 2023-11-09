import React from 'react'
import Card from '@/components/Card'
import { usePage } from '@/hooks/usePage'
import { formatTimestampDateTime } from '@/utils/datetime'
import useTranslation from '@/hooks/useTranslation'
import Table from '@/components/Table'
import { useHistory, useParams } from 'react-router-dom'
import { useFetchRuntimes } from '@/domain/runtime/hooks/useFetchRuntimes'
import User from '@/domain/user/components/User'
import { ConfirmButton, ExtendButton, QueryInput } from '@starwhale/ui'
import { VersionText } from '@starwhale/ui/Text'
import Alias from '@/components/Alias'
import { buildImageForRuntimeVersion } from '@runtime/services/runtimeVersion'
import { toaster } from 'baseui/toast'
import { useAccess } from '@/api/WithAuth'
import { getAliasStr } from '@base/utils/alias'
import { removeRuntime } from '@/domain/runtime/services/runtime'
import Shared from '@/components/Shared'
import _ from 'lodash'
import { IRuntimeSchema } from '@/domain/runtime/schemas/runtime'

export default function RuntimeListCard() {
    const [page] = usePage()
    const history = useHistory()
    const [name, setName] = React.useState('')
    const { projectId } = useParams<{ runtimeId: string; projectId: string }>()

    const runtimesInfo = useFetchRuntimes(projectId, {
        ...page,
        name,
    })

    const [t] = useTranslation()

    const isAccessRuntimeImageBuild = useAccess('runtime.image.build')
    const isAccessRUntimeDelete = useAccess('runtime.delete')

    const getActions = (runtime: IRuntimeSchema) => [
        {
            access: true,
            quickAccess: true,
            component: ({ hasText }) => (
                <ExtendButton
                    isFull
                    icon='Detail'
                    tooltip={!hasText ? t('View Details') : undefined}
                    styleas={['menuoption', hasText ? undefined : 'highlight']}
                    onClick={() =>
                        history.push(
                            `/projects/${projectId}/runtimes/${runtime.id}/versions/${runtime.version?.id}/meta`
                        )
                    }
                >
                    {hasText ? t('View Details') : undefined}
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
                    onClick={() => history.push(`/projects/${projectId}/runtimes/${runtime.id}`)}
                >
                    {hasText ? t('Version History') : undefined}
                </ExtendButton>
            ),
        },
        {
            access: isAccessRuntimeImageBuild,
            component: ({ hasText }) => {
                const isBuilt = !!runtime.version?.builtImage
                const text = isBuilt ? t('runtime.image.built') : t('runtime.image.build')

                return (
                    <ExtendButton
                        disabled={isBuilt}
                        styleas={['menuoption', hasText ? undefined : 'highlight', isBuilt ? 'icondisable' : undefined]}
                        icon={isBuilt ? 'a-ImageBuilt' : 'a-BuildImage'}
                        onClick={async () => {
                            const result = await buildImageForRuntimeVersion(projectId, runtime.id, runtime.version.id)
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
                        {hasText ? text : undefined}
                    </ExtendButton>
                )
            },
        },
        {
            access: isAccessRUntimeDelete,
            component: ({ hasText }) => (
                <ConfirmButton
                    title={`${runtime.name} ${t('runtime.remove.confirm')}`}
                    isFull
                    icon='delete'
                    styleas={['menuoption', 'negative']}
                    onClick={async () => {
                        await removeRuntime(projectId, runtime.id)
                        toaster.positive(t('runtime.remove.success'), {
                            autoHideDuration: 1000,
                        })
                        runtimesInfo.refetch()
                    }}
                >
                    {hasText ? t('runtime.remove.button') : undefined}
                </ConfirmButton>
            ),
        },
    ]

    return (
        <Card title={t('Runtimes')}>
            <div className='max-w-280px mb-10px'>
                <QueryInput
                    placeholder={t('model.search.name.placeholder')}
                    onChange={_.debounce((val: string) => {
                        setName(val.trim())
                    }, 100)}
                />
            </div>
            <Table
                renderActions={(rowIndex) => {
                    const data = runtimesInfo.data?.list[rowIndex]
                    if (!data) return undefined
                    return getActions(data)
                }}
                isLoading={runtimesInfo.isLoading}
                columns={[
                    t('sth name', [t('Runtime')]),
                    t('latest.version'),
                    t('latest.version.alias'),
                    t('Shared'),
                    t('Image'),
                    t('Owner'),
                    t('Created'),
                ]}
                data={
                    runtimesInfo.data?.list.map((runtime) => {
                        return [
                            runtime.name,
                            <VersionText key='name' version={runtime.version?.name ?? '-'} />,
                            runtime.version && <Alias key='alias' alias={getAliasStr(runtime.version)} />,
                            <Shared key='shared' shared={runtime.version?.shared} isTextShow />,
                            runtime.version?.image ?? '-',
                            runtime.owner && <User user={runtime.owner} />,
                            runtime.version?.createdTime && formatTimestampDateTime(runtime.version?.createdTime),
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
