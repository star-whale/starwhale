import React, { useCallback, useState } from 'react'
import Card from '@/components/Card'
import { createDataset } from '@dataset/services/dataset'
import { usePage } from '@/hooks/usePage'
import { ICreateDatasetSchema } from '@dataset/schemas/dataset'
import DatasetForm from '@dataset/components/DatasetForm'
import { formatTimestampDateTime } from '@/utils/datetime'
import useTranslation from '@/hooks/useTranslation'
import User from '@/domain/user/components/User'
import { Modal, ModalHeader, ModalBody } from 'baseui/modal'
import Table from '@/components/Table'
import { Link, useParams } from 'react-router-dom'
import { useFetchDatasets } from '@dataset/hooks/useFetchDatasets'

export default function DatasetListCard() {
    const [page] = usePage()
    const { projectId } = useParams<{ datasetId: string; projectId: string }>()

    const datasetsInfo = useFetchDatasets(projectId, page)
    const [isCreateDatasetOpen, setIsCreateDatasetOpen] = useState(false)
    const handleCreateDataset = useCallback(
        async (data: ICreateDatasetSchema) => {
            await createDataset(projectId, data)
            await datasetsInfo.refetch()
            setIsCreateDatasetOpen(false)
        },
        [datasetsInfo, projectId]
    )
    const [t] = useTranslation()

    return (
        <Card title={t('Datasets')}>
            <Table
                isLoading={datasetsInfo.isLoading}
                columns={[t('sth name', [t('Dataset')]), t('Owner'), t('Created'), t('Action')]}
                data={
                    datasetsInfo.data?.list.map((dataset) => {
                        return [
                            <Link key={dataset.id} to={`/projects/${projectId}/datasets/${dataset.id}`}>
                                {dataset.name}
                            </Link>,
                            dataset.owner && <User user={dataset.owner} />,
                            dataset.createdTime && formatTimestampDateTime(dataset.createdTime),
                            <Link key={dataset.id} to={`/projects/${projectId}/datasets/${dataset.id}/versions`}>
                                {t('Version History')}
                            </Link>,
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
            <Modal
                isOpen={isCreateDatasetOpen}
                onClose={() => setIsCreateDatasetOpen(false)}
                closeable
                animate
                autoFocus
            >
                <ModalHeader>{t('create sth', [t('Dataset')])}</ModalHeader>
                <ModalBody>
                    <DatasetForm onSubmit={handleCreateDataset} />
                </ModalBody>
            </Modal>
        </Card>
    )
}
