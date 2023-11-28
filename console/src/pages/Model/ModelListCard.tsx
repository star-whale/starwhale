import React from 'react'
import Card from '@/components/Card'
import { removeModel } from '@model/services/model'
import { usePage } from '@/hooks/usePage'
import { formatTimestampDateTime } from '@/utils/datetime'
import useTranslation from '@/hooks/useTranslation'
import User from '@/domain/user/components/User'
import Table from '@/components/Table'
import { useHistory, useParams } from 'react-router-dom'
import { useFetchModels } from '@model/hooks/useFetchModels'
import { ConfirmButton, ExtendButton, QueryInput } from '@starwhale/ui'
import { useAccess, WithCurrentAuth } from '@/api/WithAuth'
import { VersionText } from '@starwhale/ui/Text'
import Alias from '@/components/Alias'
import { getAliasStr } from '@base/utils/alias'
import { toaster } from 'baseui/toast'
import { getReadableStorageQuantityStr } from '@starwhale/ui/utils'
import Shared from '@/components/Shared'
import _ from 'lodash'
import QuickStartNewModel from '@/domain/project/components/QuickStartNewModel'
import { IModelVo } from '@/api'
import { IHasTagSchema } from '@base/schemas/resource'

export default function ModelListCard() {
    const [page] = usePage()
    const { projectId } = useParams<{ modelId: string; projectId: string }>()
    const history = useHistory()
    const [name, setName] = React.useState('')
    const modelsInfo = useFetchModels(projectId, {
        ...page,
        name,
    })
    const [t] = useTranslation()

    const isAccessModelRun = useAccess('model.run')
    const isAccessModelDelete = useAccess('model.delete')
    const isAccessOnlineEval = false
    const getActions = (model: IModelVo) => [
        {
            access: true,
            quickAccess: true,
            component: ({ hasText }) => (
                <ExtendButton
                    isFull
                    icon='Detail'
                    styleas={['menuoption', hasText ? undefined : 'highlight']}
                    tooltip={!hasText ? t('View Details') : undefined}
                    onClick={() =>
                        history.push(`/projects/${projectId}/models/${model.id}/versions/${model.version?.id}/overview`)
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
                    onClick={() => history.push(`/projects/${projectId}/models/${model.id}/versions`)}
                >
                    {hasText ? t('Version History') : undefined}
                </ExtendButton>
            ),
        },
        {
            access: isAccessModelRun,
            component: ({ hasText }) => (
                <ExtendButton
                    isFull
                    tooltip={t('model.run')}
                    icon='a-runmodel'
                    styleas={['menuoption', hasText ? undefined : 'highlight']}
                    onClick={() => history.push(`/projects/${projectId}/new_job/?modelId=${model.id}`)}
                >
                    {hasText ? t('model.run') : undefined}
                </ExtendButton>
            ),
        },
        {
            access: isAccessOnlineEval,
            component: ({ hasText }) => (
                <WithCurrentAuth id='online-eval'>
                    {(isPrivileged: boolean, isCommunity: boolean) => {
                        if (!isPrivileged) return null
                        if (!isCommunity)
                            return (
                                <ExtendButton
                                    isFull
                                    icon='a-onlineevaluation'
                                    styleas={['menuoption', hasText ? undefined : 'highlight']}
                                    onClick={() =>
                                        history.push(
                                            `/projects/${projectId}/new_job/?modelId=${model.id}&modelVersionHandler=serving`
                                        )
                                    }
                                >
                                    {hasText ? t('online eval') : undefined}
                                </ExtendButton>
                            )

                        return (
                            <ExtendButton
                                isFull
                                icon='a-onlineevaluation'
                                styleas={['menuoption']}
                                as={hasText ? undefined : 'link'}
                                onClick={() => history.push(`/projects/${projectId}/online_eval/${model.id}`)}
                            >
                                {hasText ? t('online eval') : undefined}
                            </ExtendButton>
                        )
                    }}
                </WithCurrentAuth>
            ),
        },
        {
            access: isAccessModelDelete,
            component: ({ hasText }) => (
                <ConfirmButton
                    title={`${model.name} ${t('model.remove.confirm')}`}
                    styleas={['menuoption', 'negative']}
                    icon='delete'
                    isFull
                    onClick={async () => {
                        await removeModel(projectId, model.id)
                        toaster.positive(t('model.remove.success'), { autoHideDuration: 1000 })
                        modelsInfo.refetch()
                    }}
                >
                    {hasText ? t('model.remove.button') : undefined}
                </ConfirmButton>
            ),
        },
    ]

    return (
        <Card
            title={
                <div className='flex items-center gap-20px'>
                    <p className='font-bold text-18px'>{t('Models')}</p>
                    <QuickStartNewModel />
                </div>
            }
        >
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
                    const model = modelsInfo.data?.list?.[rowIndex]
                    if (!model) return undefined
                    return getActions(model)
                }}
                isLoading={modelsInfo.isLoading}
                columns={[
                    t('sth name', [t('Model')]),
                    t('latest.version'),
                    t('latest.version.alias'),
                    t('Shared'),
                    t('Size'),
                    t('Owner'),
                    t('Created'),
                ]}
                data={
                    modelsInfo.data?.list?.map((model) => {
                        return [
                            model.name,
                            <VersionText key='name' version={model.version?.name ?? '-'} />,
                            model.version && <Alias key='alias' alias={getAliasStr(model.version as IHasTagSchema)} />,
                            <Shared key='shared' shared={model.version?.shared} isTextShow />,
                            model.version && getReadableStorageQuantityStr(Number(model.version.size)),
                            model.owner && <User user={model.owner} />,
                            model.version?.createdTime && formatTimestampDateTime(model.version?.createdTime),
                        ]
                    }) ?? []
                }
                paginationProps={{
                    start: modelsInfo.data?.pageNum,
                    count: modelsInfo.data?.pageSize,
                    total: modelsInfo.data?.total,
                    afterPageChange: () => {
                        modelsInfo.refetch()
                    },
                }}
            />
        </Card>
    )
}
