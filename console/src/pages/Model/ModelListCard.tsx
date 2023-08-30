import React, { useCallback, useState } from 'react'
import Card from '@/components/Card'
import { createModel, removeModel } from '@model/services/model'
import { usePage } from '@/hooks/usePage'
import { ICreateModelSchema } from '@model/schemas/model'
import ModelForm from '@model/components/ModelForm'
import { formatTimestampDateTime } from '@/utils/datetime'
import useTranslation from '@/hooks/useTranslation'
import User from '@/domain/user/components/User'
import { Modal, ModalBody, ModalHeader } from 'baseui/modal'
import Table from '@/components/Table'
import { useHistory, useParams } from 'react-router-dom'
import { useFetchModels } from '@model/hooks/useFetchModels'
import { TextLink } from '@/components/Link'
import { ButtonGroup, ConfirmButton, ExtendButton } from '@starwhale/ui'
import { WithCurrentAuth } from '@/api/WithAuth'
import { MonoText } from '@starwhale/ui/Text'
import Alias from '@/components/Alias'
import { getAliasStr } from '@base/utils/alias'
import { toaster } from 'baseui/toast'
import { getReadableStorageQuantityStr } from '@starwhale/ui/utils'

export default function ModelListCard() {
    const [page] = usePage()
    const { projectId } = useParams<{ modelId: string; projectId: string }>()
    const history = useHistory()

    const modelsInfo = useFetchModels(projectId, page)
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

    return (
        <Card title={t('Models')}>
            <Table
                isLoading={modelsInfo.isLoading}
                columns={[
                    t('sth name', [t('Model')]),
                    t('Model Version'),
                    t('Alias'),
                    t('Size'),
                    t('Owner'),

                    t('Created'),
                    t('Action'),
                ]}
                data={
                    modelsInfo.data?.list.map((model) => {
                        return [
                            <TextLink
                                key={model.id}
                                to={`/projects/${projectId}/models/${model.id}/versions/${model.version?.id}/overview`}
                            >
                                {model.name}
                            </TextLink>,
                            <MonoText key='name'>{model.version?.name ?? '-'}</MonoText>,
                            model.version && <Alias key='alias' alias={getAliasStr(model.version)} />,
                            model.version && getReadableStorageQuantityStr(Number(model.version.size)),
                            model.owner && <User user={model.owner} />,
                            model.createdTime && formatTimestampDateTime(model.createdTime),
                            <ButtonGroup key='action'>
                                <ExtendButton
                                    tooltip={t('Version History')}
                                    icon='a-Versionhistory'
                                    as='link'
                                    onClick={() => history.push(`/projects/${projectId}/models/${model.id}/versions`)}
                                />
                                <WithCurrentAuth id='model.run'>
                                    <ExtendButton
                                        tooltip={t('model.run')}
                                        icon='a-runmodel'
                                        as='link'
                                        onClick={() =>
                                            history.push(`/projects/${projectId}/new_job/?modelId=${model.id}`)
                                        }
                                    />
                                </WithCurrentAuth>
                                <WithCurrentAuth id='online-eval'>
                                    {(isPrivileged: boolean, isCommunity: boolean) => {
                                        if (!isPrivileged) return null
                                        if (!isCommunity)
                                            return (
                                                <ExtendButton
                                                    tooltip={t('online eval')}
                                                    icon='a-onlineevaluation'
                                                    as='link'
                                                    onClick={() =>
                                                        history.push(
                                                            `/projects/${projectId}/new_job/?modelId=${model.id}&modelVersionHandler=serving`
                                                        )
                                                    }
                                                />
                                            )

                                        return (
                                            <ExtendButton
                                                tooltip={t('online eval')}
                                                icon='a-onlineevaluation'
                                                as='link'
                                                onClick={() =>
                                                    history.push(`/projects/${projectId}/online_eval/${model.id}`)
                                                }
                                            />
                                        )
                                    }}
                                </WithCurrentAuth>
                                <WithCurrentAuth id='model.delete'>
                                    <ConfirmButton
                                        title={t('model.remove.confirm')}
                                        tooltip={t('model.remove.button')}
                                        as='link'
                                        negative
                                        icon='delete'
                                        onClick={async () => {
                                            await removeModel(projectId, model.id)
                                            toaster.positive(t('model.remove.success'), { autoHideDuration: 1000 })
                                            history.push(`/projects/${projectId}/models`)
                                        }}
                                    />
                                </WithCurrentAuth>
                            </ButtonGroup>,
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
