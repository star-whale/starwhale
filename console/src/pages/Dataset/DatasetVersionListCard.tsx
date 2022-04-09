import React, { useCallback, useState } from 'react'
import Card from '@/components/Card'
import { createDatasetVersion } from '@dataset/services/datasetVersion'
import { usePage } from '@/hooks/usePage'
import { ICreateDatasetVersionSchema } from '@dataset/schemas/datasetVersion'
import DatasetVersionForm from '@dataset/components/DatasetVersionForm'
import { formatTimestampDateTime } from '@/utils/datetime'
import useTranslation from '@/hooks/useTranslation'
import { Button, SIZE as ButtonSize } from 'baseui/button'
import User from '@/components/User'
import { Modal, ModalHeader, ModalBody } from 'baseui/modal'
import Table from '@/components/Table'
import { Link, useParams } from 'react-router-dom'
import { useFetchDatasetVersions } from '@dataset/hooks/useFetchDatasetVersions'
import { resourceIconMapping } from '@/consts'
import { useDataset } from '@dataset/hooks/useDataset'

export default function DatasetVersionListCard() {
    const [page] = usePage()
    const { datasetId, projectId } = useParams<{ datasetId: string; projectId: string }>()
    const { dataset } = useDataset()

    const datasetsInfo = useFetchDatasetVersions(projectId, datasetId, page)
    const [isCreateDatasetVersionOpen, setIsCreateDatasetVersionOpen] = useState(false)
    const handleCreateDatasetVersion = useCallback(
        async (data: ICreateDatasetVersionSchema) => {
            await createDatasetVersion(projectId, datasetId, data)
            await datasetsInfo.refetch()
            setIsCreateDatasetVersionOpen(false)
        },
        [datasetsInfo]
    )
    const [t] = useTranslation()

    return (
        <Card
            title={t('dataset versions')}
            extra={
                <Button size={ButtonSize.compact} onClick={() => setIsCreateDatasetVersionOpen(true)}>
                    {t('create')}
                </Button>
            }
        >
            <Table
                isLoading={datasetsInfo.isLoading}
                // t('sth name', [t('Dataset Version')]),
                columns={[t('Tag'), t('Created'), t('Owner'), t('Action')]}
                data={
                    datasetsInfo.data?.list.map((dataset) => {
                        return [
                            // dataset.Version,
                            dataset.tag,
                            dataset.createTime && formatTimestampDateTime(dataset.createTime),
                            dataset.owner && <User user={dataset.owner} />,
                            <Button size='mini' key={dataset.id} onClick={() => {}}>
                                {t('Revert')}
                            </Button>,
                        ]
                    }) ?? []
                }
                paginationProps={{
                    start: datasetsInfo.data?.pageNum,
                    count: datasetsInfo.data?.size,
                    total: datasetsInfo.data?.total,
                    afterPageChange: () => {
                        datasetsInfo.refetch()
                    },
                }}
            />
            <Modal
                isOpen={isCreateDatasetVersionOpen}
                onClose={() => setIsCreateDatasetVersionOpen(false)}
                closeable
                animate
                autoFocus
                unstable_ModalBackdropScroll
            >
                <ModalHeader>{t('create sth', [t('Dataset Version')])}</ModalHeader>
                <ModalBody>
                    <DatasetVersionForm onSubmit={handleCreateDatasetVersion} />
                </ModalBody>
            </Modal>
        </Card>
    )
}
