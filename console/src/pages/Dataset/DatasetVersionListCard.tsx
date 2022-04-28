import React, { useCallback, useState, useRef, useMemo } from 'react'
import Card from '@/components/Card'
import { createDatasetVersion } from '@dataset/services/datasetVersion'
import { usePage } from '@/hooks/usePage'
import { ICreateDatasetVersionSchema } from '@dataset/schemas/datasetVersion'
import DatasetVersionForm from '@dataset/components/DatasetVersionForm'
import { formatTimestampDateTime } from '@/utils/datetime'
import useTranslation from '@/hooks/useTranslation'
import { Button, SIZE as ButtonSize } from 'baseui/button'
import User from '@/domain/user/components/User'
import { Modal, ModalHeader, ModalBody } from 'baseui/modal'
import Table from '@/components/Table'
import { Link, useParams } from 'react-router-dom'
import { useFetchDatasetVersions } from '@dataset/hooks/useFetchDatasetVersions'
import { useDataset } from '@dataset/hooks/useDataset'
import { Drawer } from 'baseui/drawer'
import { JSONTree } from 'react-json-tree'

//@ts-ignore
import yaml from 'js-yaml'

export default function DatasetVersionListCard() {
    const [page] = usePage()
    const { datasetId, projectId } = useParams<{ datasetId: string; projectId: string }>()
    const { dataset } = useDataset()
    const [isOpen, setIsOpen] = useState(false)
    const [drawerData, setDrawerData] = useState('')

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

    const cardRef = useRef(null)

    const jsonData = useMemo(() => {
        if (!drawerData) return {}
        return yaml.load(drawerData)
    }, [drawerData])

    return (
        <div
            style={{
                width: '100%',
                height: 'auto',
            }}
            ref={cardRef}
        >
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
                    columns={[t('Meta'), t('Created'), t('Owner'), t('Action')]}
                    data={
                        datasetsInfo.data?.list.map((dataset) => {
                            return [
                                // dataset.Version,
                                <Button
                                    size='mini'
                                    onClick={() => {
                                        setDrawerData(dataset.meta)
                                        setIsOpen(true)
                                    }}
                                >
                                    {t('show meta')}
                                </Button>,
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
                        count: datasetsInfo.data?.pageSize,
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
                >
                    <ModalHeader>{t('create sth', [t('Dataset Version')])}</ModalHeader>
                    <ModalBody>
                        <DatasetVersionForm onSubmit={handleCreateDatasetVersion} />
                    </ModalBody>
                </Modal>
            </Card>
            {cardRef.current && (
                <Drawer
                    size='50%'
                    isOpen={isOpen}
                    autoFocus
                    onClose={() => setIsOpen(false)}
                    mountNode={cardRef.current}
                    overrides={{
                        DrawerContainer: {
                            style: {
                                boxSizing: 'border-box',
                                padding: '70px 0 10px',
                            },
                        },
                        DrawerBody: {
                            style: { backgroundColor: 'rgb(0, 43, 54)' },
                        },
                    }}
                >
                    <div style={{ padding: '10px', backgroundColor: 'rgb(0, 43, 54)' }}>
                        <JSONTree data={jsonData} />
                    </div>
                </Drawer>
            )}
        </div>
    )
}
