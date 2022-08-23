import React, { useCallback, useState, useRef, useMemo } from 'react'
import Card from '@/components/Card'
import { createDatasetVersion, revertDatasetVersion } from '@dataset/services/datasetVersion'
import { usePage } from '@/hooks/usePage'
import { ICreateDatasetVersionSchema } from '@dataset/schemas/datasetVersion'
import DatasetVersionForm from '@dataset/components/DatasetVersionForm'
import { formatTimestampDateTime } from '@/utils/datetime'
import useTranslation from '@/hooks/useTranslation'
import User from '@/domain/user/components/User'
import { Modal, ModalHeader, ModalBody } from 'baseui/modal'
import Table from '@/components/Table'
import { useParams } from 'react-router-dom'
import { useFetchDatasetVersions } from '@dataset/hooks/useFetchDatasetVersions'
import { Drawer } from 'baseui/drawer'
import { JSONTree } from 'react-json-tree'

// eslint-disable-next-line
import yaml from 'js-yaml'
import { StyledLink } from 'baseui/link'
import { toaster } from 'baseui/toast'
import Button from '@/components/Button'

export default function DatasetVersionListCard() {
    const [page] = usePage()
    const { datasetId, projectId } = useParams<{ datasetId: string; projectId: string }>()
    const [isOpen, setIsOpen] = useState(false)
    const [drawerData, setDrawerData] = useState('')
    const [t] = useTranslation()

    const datasetVersionsInfo = useFetchDatasetVersions(projectId, datasetId, page)
    const [isCreateDatasetVersionOpen, setIsCreateDatasetVersionOpen] = useState(false)
    const handleCreateDatasetVersion = useCallback(
        async (data: ICreateDatasetVersionSchema) => {
            await createDatasetVersion(projectId, datasetId, data)
            await datasetVersionsInfo.refetch()
            setIsCreateDatasetVersionOpen(false)
        },
        [datasetVersionsInfo, datasetId, projectId]
    )

    const handleAction = useCallback(
        async (datasetVersionId) => {
            await revertDatasetVersion(projectId, datasetId, datasetVersionId)
            toaster.positive(t('data version revert done'), { autoHideDuration: 2000 })
            await datasetVersionsInfo.refetch()
        },
        [datasetVersionsInfo, projectId, datasetId, t]
    )

    const cardRef = useRef(null)

    const jsonData = useMemo(() => {
        if (!drawerData) return {}
        return yaml.load(drawerData)
    }, [drawerData])

    return (
        <>
            <Card>
                <Table
                    isLoading={datasetVersionsInfo.isLoading}
                    columns={[t('sth name'), t('Meta'), t('Created'), t('Owner'), t('Action')]}
                    data={
                        datasetVersionsInfo.data?.list.map((datasetVersion) => {
                            return [
                                datasetVersion.name,
                                <Button
                                    key={datasetVersion.id}
                                    size='compact'
                                    as='link'
                                    onClick={() => {
                                        setDrawerData(datasetVersion.meta)
                                        setIsOpen(true)
                                    }}
                                >
                                    {t('show meta')}
                                </Button>,
                                datasetVersion.createdTime && formatTimestampDateTime(datasetVersion.createdTime),
                                datasetVersion.owner && <User user={datasetVersion.owner} />,
                                <StyledLink
                                    animateUnderline={false}
                                    className='row-center--inline gap4'
                                    key={datasetVersion.id}
                                    onClick={() => {
                                        handleAction(datasetVersion.id)
                                    }}
                                >
                                    {t('Revert')}
                                </StyledLink>,
                            ]
                        }) ?? []
                    }
                    paginationProps={{
                        start: datasetVersionsInfo.data?.pageNum,
                        count: datasetVersionsInfo.data?.pageSize,
                        total: datasetVersionsInfo.data?.total,
                        afterPageChange: () => {
                            datasetVersionsInfo.refetch()
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
                        <JSONTree data={jsonData} hideRoot shouldExpandNode={() => true} />
                    </div>
                </Drawer>
            )}
        </>
    )
}
