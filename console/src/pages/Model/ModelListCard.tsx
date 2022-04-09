import React, { useCallback, useState } from 'react'
import Card from '@/components/Card'
import { createModel } from '@model/services/model'
import { usePage } from '@/hooks/usePage'
import { ICreateModelSchema } from '@model/schemas/model'
import ModelForm from '@model/components/ModelForm'
import { formatTimestampDateTime } from '@/utils/datetime'
import useTranslation from '@/hooks/useTranslation'
import { Button, SIZE as ButtonSize } from 'baseui/button'
import User from '@/components/User'
import { Modal, ModalHeader, ModalBody } from 'baseui/modal'
import Table from '@/components/Table'
import { Link, useParams } from 'react-router-dom'
import { useFetchModels } from '@model/hooks/useFetchModels'
import { resourceIconMapping } from '@/consts'

export default function ModelListCard() {
    const [page] = usePage()
    const { modelId, projectId } = useParams<{ modelId: string; projectId: string }>()

    const modelsInfo = useFetchModels(projectId, page)
    const [isCreateModelOpen, setIsCreateModelOpen] = useState(false)
    const handleCreateModel = useCallback(
        async (data: ICreateModelSchema) => {
            await createModel(projectId, data)
            await modelsInfo.refetch()
            setIsCreateModelOpen(false)
        },
        [modelsInfo]
    )
    const [t] = useTranslation()

    return (
        <Card
            title={t('models')}
            extra={
                <Button size={ButtonSize.compact} onClick={() => setIsCreateModelOpen(true)}>
                    {t('create')}
                </Button>
            }
        >
            <Table
                isLoading={modelsInfo.isLoading}
                columns={[t('sth name', [t('Model')]), t('Owner'), t('Created'), t('Action')]}
                data={
                    modelsInfo.data?.list.map((model) => {
                        return [
                            <Link key={model.id} to={`/projects/${projectId}/models/${model.id}`}>
                                {model.name}
                            </Link>,
                            model.owner && <User user={model.owner} />,
                            model.createTime && formatTimestampDateTime(model.createTime),
                            <Link key={model.id} to={`/projects/${projectId}/models/${model.id}/versions`}>
                                {t('Version History')}
                            </Link>,
                        ]
                    }) ?? []
                }
                paginationProps={{
                    start: modelsInfo.data?.pageNum,
                    count: modelsInfo.data?.size,
                    total: modelsInfo.data?.total,
                    afterPageChange: () => {
                        modelsInfo.refetch()
                    },
                }}
            />
            <Modal
                isOpen={isCreateModelOpen}
                onClose={() => setIsCreateModelOpen(false)}
                closeable
                animate
                autoFocus
                unstable_ModalBackdropScroll
            >
                <ModalHeader>{t('create sth', [t('Model')])}</ModalHeader>
                <ModalBody>
                    <ModelForm onSubmit={handleCreateModel} />
                </ModalBody>
            </Modal>
        </Card>
    )
}
