import React from 'react'
import { usePage } from '@/hooks/usePage'
import { formatTimestampDateTime } from '@/utils/datetime'
import useTranslation from '@/hooks/useTranslation'
import User from '@/domain/user/components/User'
import Table from '@/components/Table'
import { useParams } from 'react-router-dom'
import { useFetchRuntimeVersions } from '@/domain/runtime/hooks/useFetchRuntimeVersions'
import Button from '@starwhale/ui/Button'
import { IRuntimeDetailSchema } from '@/domain/runtime/schemas/runtime'
import { revertRuntimeVersion } from '@/domain/runtime/services/runtimeVersion'
import { toaster } from 'baseui/toast'
import { WithCurrentAuth } from '@/api/WithAuth'
import { TextLink } from '@/components/Link'
import CopyToClipboard from '@/components/CopyToClipboard/CopyToClipboard'
import Alias from '@/components/Alias'
import Shared from '@/components/Shared'

export default function RuntimeVersionListCard() {
    const [page] = usePage()
    const { runtimeId, projectId } = useParams<{ runtimeId: string; projectId: string }>()
    const runtimesInfo = useFetchRuntimeVersions(projectId, runtimeId, page)
    const [t] = useTranslation()
    const handleRevert = React.useCallback(
        async (data: IRuntimeDetailSchema) => {
            await revertRuntimeVersion(projectId, runtimeId, data.id as string)
            toaster.positive(t('runtime version reverted'), { autoHideDuration: 2000 })
            await runtimesInfo.refetch()
        },
        [runtimesInfo, projectId, runtimeId, t]
    )

    return (
        <Table
            isLoading={runtimesInfo.isLoading}
            columns={[
                t('Runtime Version'),
                t('Alias'),
                t('Shared'),
                // {
                //     type: 'tags',
                //     title: t('Tag'),
                //     minWidth: 200,
                //     onAsyncChange: async (value: any, columnIndex: number, rowIndex: number) => {
                //         const data = runtimesInfo.data?.list?.[rowIndex]
                //         try {
                //             await updateRuntimeVersion(projectId, runtimeId, data?.id as string, { tag: value })
                //             await runtimesInfo.refetch()
                //         } catch (e) {
                //             // console.error(e)
                //         }
                //     },
                //     mapDataToValue: (item: any) => {
                //         // tag index
                //         return item[1] ?? ''
                //     },
                // },
                // t('Tag'),
                t('Created'),
                t('Owner'),
                t('Action'),
            ]}
            data={
                runtimesInfo.data?.list.map((runtime, i) => {
                    return [
                        <TextLink
                            key={runtime.id}
                            to={`/projects/${projectId}/runtimes/${runtimeId}/versions/${runtime.id}/overview`}
                        >
                            {runtime.name}
                        </TextLink>,
                        <Alias key='alias' alias={runtime.alias} />,
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
