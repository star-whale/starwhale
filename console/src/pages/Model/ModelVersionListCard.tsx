import React, { useCallback, useState } from 'react'
import Card from '@/components/Card'
import { createModelVersion } from '@model/services/modelVersion'
import { usePage } from '@/hooks/usePage'
import { ICreateModelVersionSchema } from '@model/schemas/modelVersion'
import ModelVersionForm from '@model/components/ModelVersionForm'
import { formatTimestampDateTime } from '@/utils/datetime'
import useTranslation from '@/hooks/useTranslation'
import { Button, SIZE as ButtonSize } from 'baseui/button'
import User from '@/domain/user/components/User'
import { Modal, ModalHeader, ModalBody } from 'baseui/modal'
import Table from '@/components/Table'
import { Link, useParams } from 'react-router-dom'
import { useFetchModelVersions } from '@model/hooks/useFetchModelVersions'
import { useModel } from '@model/hooks/useModel'

export default function ModelVersionListCard() {
    const [page] = usePage()
    const { modelId, projectId } = useParams<{ modelId: string; projectId: string }>()
    const { model } = useModel()

    const modelsInfo = useFetchModelVersions(projectId, modelId, page)
    const [isCreateModelVersionOpen, setIsCreateModelVersionOpen] = useState(false)
    const handleCreateModelVersion = useCallback(
        async (data: ICreateModelVersionSchema) => {
            await createModelVersion(projectId, modelId, data)
            await modelsInfo.refetch()
            setIsCreateModelVersionOpen(false)
        },
        [modelsInfo]
    )
    const [t] = useTranslation()

    return (
        <Card
            title={t('model versions')}
            extra={
                <Button size={ButtonSize.compact} onClick={() => setIsCreateModelVersionOpen(true)}>
                    {t('create')}
                </Button>
            }
        >
            <Table
                isLoading={modelsInfo.isLoading}
                columns={[t('Meta'), t('Created'), t('Owner'), t('Action')]}
                data={
                    modelsInfo.data?.list.map((model) => {
                        return [
                            model.meta,
                            model.createTime && formatTimestampDateTime(model.createTime),
                            model.owner && <User user={model.owner} />,
                            <Button size='mini' key={model.id} onClick={() => {}}>
                                {t('Revert')}
                            </Button>,
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
            <Modal
                isOpen={isCreateModelVersionOpen}
                onClose={() => setIsCreateModelVersionOpen(false)}
                closeable
                animate
                autoFocus
            >
                <ModalHeader>{t('create sth', [t('Model Version')])}</ModalHeader>
                <ModalBody>
                    <ModelVersionForm onSubmit={handleCreateModelVersion} />
                </ModalBody>
            </Modal>
        </Card>
    )
}
