import React, { useCallback } from 'react'
import Card from '@/components/Card'
import { addDatasetVersionTag, deleteDatasetVersionTag, revertDatasetVersion } from '@dataset/services/datasetVersion'
import { usePage } from '@/hooks/usePage'
import { formatTimestampDateTime } from '@/utils/datetime'
import useTranslation from '@/hooks/useTranslation'
import User from '@/domain/user/components/User'
import Table from '@/components/Table'
import { useParams } from 'react-router-dom'
import { useFetchDatasetVersions } from '@dataset/hooks/useFetchDatasetVersions'
import { toaster } from 'baseui/toast'
import { TextLink } from '@/components/Link'
import { useAccess, WithCurrentAuth } from '@/api/WithAuth'
import CopyToClipboard from '@/components/CopyToClipboard/CopyToClipboard'
import { ButtonGroup, ExtendButton } from '@starwhale/ui/Button'
import { Shared } from '@/components/Shared'
import useCliMate from '@/hooks/useCliMate'
import { EditableAlias } from '@/components/Alias'
import yaml from 'js-yaml'
import { VersionText } from '@starwhale/ui'
import { IHasTagSchema } from '@base/schemas/resource'

export default function DatasetVersionListCard() {
    const [page] = usePage()
    const { datasetId, projectId } = useParams<{ datasetId: string; projectId: string }>()
    const [t] = useTranslation()

    const datasetVersionsInfo = useFetchDatasetVersions(projectId, datasetId, page)
    const handleAction = useCallback(
        async (datasetVersionId) => {
            await revertDatasetVersion(projectId, datasetId, datasetVersionId)
            toaster.positive(t('dataset version reverted'), { autoHideDuration: 2000 })
            await datasetVersionsInfo.refetch()
        },
        [datasetVersionsInfo, projectId, datasetId, t]
    )

    const { hasCliMate, doPull } = useCliMate()
    const tagReadOnly = !useAccess('tag.edit')

    const handleTagAdd = useCallback(
        async (datasetVersionId: string, tag: string) => {
            await addDatasetVersionTag(projectId, datasetId, datasetVersionId, tag)
            await datasetVersionsInfo.refetch()
        },
        [datasetId, datasetVersionsInfo, projectId]
    )

    const handelTagRemove = useCallback(
        async (datasetVersionId: string, tag: string) => {
            await deleteDatasetVersionTag(projectId, datasetId, datasetVersionId, tag)
            await datasetVersionsInfo.refetch()
        },
        [datasetId, datasetVersionsInfo, projectId]
    )

    return (
        <>
            <Card>
                <Table
                    isLoading={datasetVersionsInfo.isLoading}
                    columns={[
                        t('sth name'),
                        t('Alias'),
                        t('Shared'),
                        t('dataset.file.count'),
                        t('Created'),
                        t('Owner'),
                        t('Action'),
                    ]}
                    data={
                        datasetVersionsInfo.data?.list?.map((datasetVersion, i) => {
                            let counts
                            try {
                                const meta = yaml.load(datasetVersion.meta || '') as any
                                counts = meta?.dataset_summary?.rows
                                // eslint-disable-next-line no-empty
                            } catch (e) {}

                            return [
                                <TextLink
                                    key={datasetId}
                                    to={`/projects/${projectId}/datasets/${datasetId}/versions/${datasetVersion.id}/overview`}
                                >
                                    <VersionText version={datasetVersion.name} />
                                </TextLink>,
                                <EditableAlias
                                    key='alias'
                                    resource={datasetVersion as IHasTagSchema}
                                    readOnly={tagReadOnly}
                                    onAddTag={(tag) => handleTagAdd(datasetVersion.id, tag)}
                                    onRemoveTag={(tag) => handelTagRemove(datasetVersion.id, tag)}
                                />,
                                <Shared key='shared' shared={datasetVersion.shared} isTextShow />,
                                counts,
                                datasetVersion.createdTime && formatTimestampDateTime(datasetVersion.createdTime),
                                datasetVersion.owner && <User user={datasetVersion.owner} />,
                                <ButtonGroup key='action'>
                                    <CopyToClipboard
                                        content={`${window.location.protocol}//${window.location.host}/projects/${projectId}/datasets/${datasetId}/versions/${datasetVersion.id}/`}
                                    />
                                    {i ? (
                                        <WithCurrentAuth id='dataset.version.revert'>
                                            <ExtendButton
                                                tooltip={t('Revert')}
                                                icon='revert'
                                                as='link'
                                                key={datasetVersion.id}
                                                onClick={() => handleAction(datasetVersion.id)}
                                            />
                                        </WithCurrentAuth>
                                    ) : null}
                                    {hasCliMate && (
                                        <ExtendButton
                                            tooltip={t('Pull resource to local with cli mate')}
                                            icon='a-Pushlocal'
                                            as='link'
                                            onClick={() => {
                                                const url = `projects/${projectId}/datasets/${datasetId}/versions/${datasetVersion.id}/`
                                                doPull({ resourceUri: url })
                                            }}
                                        />
                                    )}
                                </ButtonGroup>,
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
            </Card>

            {/* {cardRef.current && (
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
            )} */}
        </>
    )
}
