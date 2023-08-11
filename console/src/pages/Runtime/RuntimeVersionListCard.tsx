import React, { useCallback } from 'react'
import { usePage } from '@/hooks/usePage'
import { formatTimestampDateTime } from '@/utils/datetime'
import useTranslation from '@/hooks/useTranslation'
import User from '@/domain/user/components/User'
import Table from '@/components/Table'
import { useParams } from 'react-router-dom'
import { useFetchRuntimeVersions } from '@/domain/runtime/hooks/useFetchRuntimeVersions'
import Button from '@starwhale/ui/Button'
import {
    addRuntimeVersionTag,
    buildImageForRuntimeVersion,
    deleteRuntimeVersionTag,
    revertRuntimeVersion,
} from '@/domain/runtime/services/runtimeVersion'
import { toaster } from 'baseui/toast'
import { useAccess, WithCurrentAuth } from '@/api/WithAuth'
import { TextLink } from '@/components/Link'
import CopyToClipboard from '@/components/CopyToClipboard/CopyToClipboard'
import { EditableAlias } from '@/components/Alias'
import Shared from '@/components/Shared'
import { MonoText } from '@/components/Text'
import useCliMate from '@/hooks/useCliMate'
import { IRuntimeVersionSchema } from '@runtime/schemas/runtimeVersion'

export default function RuntimeVersionListCard() {
    const [page] = usePage()
    const { runtimeId, projectId } = useParams<{ runtimeId: string; projectId: string }>()
    const runtimesInfo = useFetchRuntimeVersions(projectId, runtimeId, page)
    const [t] = useTranslation()
    const handleRevert = React.useCallback(
        async (data: IRuntimeVersionSchema) => {
            await revertRuntimeVersion(projectId, runtimeId, data.id)
            toaster.positive(t('runtime version reverted'), { autoHideDuration: 2000 })
            await runtimesInfo.refetch()
        },
        [runtimesInfo, projectId, runtimeId, t]
    )
    const { hasCliMate, doPull } = useCliMate()
    const tagReadOnly = !useAccess('tag.edit')

    const handleTagAdd = useCallback(
        async (runtimeVersionId: string, tag: string) => {
            await addRuntimeVersionTag(projectId, runtimeId, runtimeVersionId, tag)
            await runtimesInfo.refetch()
        },
        [projectId, runtimeId, runtimesInfo]
    )
    const handelTagRemove = useCallback(
        async (runtimeVersionId: string, tag: string) => {
            await deleteRuntimeVersionTag(projectId, runtimeId, runtimeVersionId, tag)
            await runtimesInfo.refetch()
        },
        [projectId, runtimeId, runtimesInfo]
    )

    return (
        <Table
            isLoading={runtimesInfo.isLoading}
            columns={[t('Runtime Version'), t('Alias'), t('Shared'), t('Created'), t('Owner'), t('Action')]}
            data={
                runtimesInfo.data?.list.map((runtime, i) => {
                    return [
                        <TextLink
                            key={runtime.id}
                            to={`/projects/${projectId}/runtimes/${runtimeId}/versions/${runtime.id}/overview`}
                        >
                            <MonoText>{runtime.name}</MonoText>
                        </TextLink>,
                        <EditableAlias
                            key='alias'
                            resource={runtime}
                            readOnly={tagReadOnly}
                            onAddTag={(tag) => handleTagAdd(runtime.id, tag)}
                            onRemoveTag={(tag) => handelTagRemove(runtime.id, tag)}
                        />,
                        <Shared key='shared' shared={runtime.shared} isTextShow />,
                        runtime.createdTime && formatTimestampDateTime(runtime.createdTime),
                        runtime.owner && <User user={runtime.owner} />,
                        <>
                            <CopyToClipboard
                                content={`${window.location.protocol}//${window.location.host}/projects/${projectId}/runtimes/${runtimeId}/versions/${runtime.id}/`}
                            />
                            &nbsp;&nbsp;
                            {i ? (
                                <WithCurrentAuth id='runtime.version.revert'>
                                    <Button
                                        size='mini'
                                        kind='tertiary'
                                        key={runtime.id}
                                        onClick={() => handleRevert(runtime)}
                                    >
                                        {t('Revert')}
                                    </Button>
                                </WithCurrentAuth>
                            ) : null}
                            &nbsp;&nbsp;
                            <WithCurrentAuth id='runtime.image.build'>
                                <Button
                                    size='mini'
                                    kind='tertiary'
                                    disabled={!!runtime.builtImage}
                                    onClick={async () => {
                                        const result = await buildImageForRuntimeVersion(
                                            projectId,
                                            runtime.runtimeId,
                                            runtime.id
                                        )
                                        if (result.success) {
                                            toaster.positive(result.message, {
                                                autoHideDuration: 1000,
                                            })
                                        } else {
                                            toaster.negative(result.message, {
                                                autoHideDuration: 2000,
                                            })
                                        }
                                    }}
                                >
                                    {runtime.builtImage ? t('runtime.image.built') : t('runtime.image.build')}
                                </Button>
                            </WithCurrentAuth>
                            &nbsp;&nbsp;
                            {hasCliMate && (
                                <Button
                                    size='mini'
                                    kind='tertiary'
                                    onClick={() => {
                                        const url = `projects/${projectId}/runtimes/${runtimeId}/versions/${runtime.id}/`
                                        doPull({ resourceUri: url })
                                    }}
                                >
                                    {t('Pull resource to local with cli mate')}
                                </Button>
                            )}
                        </>,
                    ]
                }) ?? []
            }
            paginationProps={{
                start: runtimesInfo.data?.pageNum,
                count: runtimesInfo.data?.pageSize,
                total: runtimesInfo.data?.total,
                afterPageChange: () => {
                    runtimesInfo.refetch()
                },
            }}
        />
    )
}
