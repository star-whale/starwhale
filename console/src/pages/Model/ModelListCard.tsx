import React, { useCallback, useState } from 'react'
import Card from '@/components/Card'
import { createModel, removeModel } from '@model/services/model'
import { usePage } from '@/hooks/usePage'
import { ICreateModelSchema, IModelSchema } from '@model/schemas/model'
import ModelForm from '@model/components/ModelForm'
import { formatTimestampDateTime } from '@/utils/datetime'
import useTranslation from '@/hooks/useTranslation'
import User from '@/domain/user/components/User'
import { Modal, ModalBody, ModalHeader } from 'baseui/modal'
import Table from '@/components/Table'
import { useHistory, useParams } from 'react-router-dom'
import { useFetchModels } from '@model/hooks/useFetchModels'
import { ConfirmButton, ExtendButton, QueryInput } from '@starwhale/ui'
import { WithCurrentAuth, useAccess } from '@/api/WithAuth'
import { VersionText } from '@starwhale/ui/Text'
import Alias from '@/components/Alias'
import { getAliasStr } from '@base/utils/alias'
import { toaster } from 'baseui/toast'
import { getReadableStorageQuantityStr } from '@starwhale/ui/utils'
import Shared from '@/components/Shared'
import _ from 'lodash'
import QuickStartNewModel from '@/domain/project/components/QuickStartNewModel'

export default function ModelListCard() {
    const [page] = usePage()
    const { projectId } = useParams<{ modelId: string; projectId: string }>()
    const history = useHistory()
    const [name, setName] = React.useState('')
    const modelsInfo = useFetchModels(projectId, {
        ...page,
        name,
    })
    const [isCreateModelOpen, setIsCreateModelOpen] = useState(false)
    const handleCreateModel = useCallback(
        async (data: ICreateModelSchema) => {
            await createModel(projectId, data)
            await modelsInfo.refetch()
            setIsCreateModelOpen(false)
        },
        [modelsInfo, projectId]
    )
    const [t] = useTranslation()

    const isAccessModelRun = useAccess('model.run')
    const isAccessModelDelete = useAccess('model.delete')
    const isAccessOnlineEval = useAccess('online-eval')
    const getActions = (model: IModelSchema) => [
        {
            icon: 'overview',
            label: t('Overview'),
            access: true,
            quickAccess: true,
            component: (hasText) => (
                <ExtendButton
                    isFull
                    icon='overview'
                    styleAs={['menuoption', hasText ? undefined : 'highlight']}
                    onClick={() =>
                        history.push(`/projects/${projectId}/models/${model.id}/versions/${model.version?.id}/overview`)
                    }
                >
                    {hasText ? t('Overview') : undefined}
                </ExtendButton>
            ),
        },
        {
            icon: 'a-Versionhistory',
            label: t('Version History'),
            access: true,
            component: (hasText) => (
                <ExtendButton
                    isFull
                    icon='a-Versionhistory'
                    styleAs={['menuoption', hasText ? undefined : 'highlight']}
                    onClick={() => history.push(`/projects/${projectId}/models/${model.id}/versions`)}
                >
                    {hasText ? t('Version History') : undefined}
                </ExtendButton>
            ),
        },
        {
            icon: 'a-runmodel',
            label: t('model.run'),
            access: isAccessModelRun,
            component: (hasText) => (
                <WithCurrentAuth id='model.run'>
                    <ExtendButton
                        isFull
                        tooltip={t('model.run')}
                        icon='a-runmodel'
                        styleAs={['menuoption', hasText ? undefined : 'highlight']}
                        onClick={() => history.push(`/projects/${projectId}/new_job/?modelId=${model.id}`)}
                    >
                        {hasText ? t('model.run') : undefined}
                    </ExtendButton>
                </WithCurrentAuth>
            ),
        },
        {
            icon: 'a-onlineevaluation',
            label: t('online eval'),
            access: isAccessOnlineEval,
            component: (hasText) => (
                <WithCurrentAuth id='online-eval'>
                    {(isPrivileged: boolean, isCommunity: boolean) => {
                        if (!isPrivileged) return null
                        if (!isCommunity)
                            return (
                                <ExtendButton
                                    isFull
                                    icon='a-onlineevaluation'
                                    styleAs={['menuoption', hasText ? undefined : 'highlight']}
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
                                styleAs={['menuoption']}
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
            icon: 'delete',
            label: t('model.remove.button'),
            access: isAccessModelDelete,
            component: (hasText) => (
                <WithCurrentAuth id='model.delete'>
                    <ConfirmButton
                        title={t('model.remove.confirm')}
                        styleAs={['menuoption', 'negative']}
                        icon='delete'
                        isFull
                        onClick={async () => {
                            await removeModel(projectId, model.id)
                            toaster.positive(t('model.remove.success'), { autoHideDuration: 1000 })
                            history.push(`/projects/${projectId}/models`)
                        }}
                    >
                        {hasText ? t('model.remove.button') : undefined}
                    </ConfirmButton>
                </WithCurrentAuth>
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
                    const model = modelsInfo.data?.list[rowIndex]
                    console.log(model, rowIndex)
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
                    modelsInfo.data?.list.map((model) => {
                        return [
                            model.name,
                            <VersionText key='name' version={model.version?.name ?? '-'} />,
                            model.version && <Alias key='alias' alias={getAliasStr(model.version)} />,
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
            <Modal isOpen={isCreateModelOpen} onClose={() => setIsCreateModelOpen(false)} closeable animate autoFocus>
                <ModalHeader>{t('create sth', [t('Model')])}</ModalHeader>
                <ModalBody>
                    <ModelForm onSubmit={handleCreateModel} />
                </ModalBody>
            </Modal>
        </Card>
    )
}
