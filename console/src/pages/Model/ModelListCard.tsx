import React, { useCallback, useState } from 'react'
import Card from '@/components/Card'
import { createModel } from '@model/services/model'
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
import { Button } from '@starwhale/ui'
import { WithCurrentAuth } from '@/api/WithAuth'

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
                columns={[t('sth name', [t('Model')]), t('Owner'), t('Created'), t('Action')]}
                data={
                    modelsInfo.data?.list.map((model) => {
                        return [
                            <TextLink
                                key={model.id}
                                to={`/projects/${projectId}/models/${model.id}/versions/${model.version?.id}/overview`}
                            >
                                {model.name}
                            </TextLink>,
                            model.owner && <User user={model.owner} />,
                            model.createdTime && formatTimestampDateTime(model.createdTime),
                            <div key='action' style={{ display: 'flex', gap: '5px' }}>
                                <Button
                                    kind='tertiary'
                                    onClick={() => history.push(`/projects/${projectId}/models/${model.id}/versions`)}
                                >
                                    {t('Version History')}
                                </Button>
                                <WithCurrentAuth id='model.run'>
                                    <Button
                                        kind='tertiary'
                                        onClick={() =>
                                            history.push(`/projects/${projectId}/new_job/?modelId=${model.id}`)
                                        }
                                    >
                                        {t('model.run')}
                                    </Button>
                                </WithCurrentAuth>
                                <WithCurrentAuth id='online-eval'>
                                    {(isPrivileged: boolean, isCommunity: boolean) => {
                                        if (!isPrivileged) return null
                                        if (!isCommunity)
                                            return (
                                                <Button
                                                    kind='tertiary'
                                                    onClick={() =>
                                                        history.push(
                                                            `/projects/${projectId}/new_job/?modelId=${model.id}&modelVersionHandler=serving`
                                                        )
                                                    }
                                                >
                                                    {t('online eval')}
                                                </Button>
                                            )

                                        return (
                                            <Button
                                                kind='tertiary'
                                                onClick={() =>
                                                    history.push(`/projects/${projectId}/online_eval/${model.id}`)
                                                }
                                            >
                                                {t('online eval')}
                                            </Button>
                                        )
                                    }}
                                </WithCurrentAuth>
                            </div>,
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
