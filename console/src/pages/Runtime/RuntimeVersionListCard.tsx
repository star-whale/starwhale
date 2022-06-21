import React from 'react'
import Card from '@/components/Card'
import { usePage } from '@/hooks/usePage'
import { formatTimestampDateTime } from '@/utils/datetime'
import useTranslation from '@/hooks/useTranslation'
import { Button } from 'baseui/button'
import User from '@/domain/user/components/User'
import Table from '@/components/Table/TableTyped'
import { useParams } from 'react-router-dom'
import { useFetchRuntimeVersions } from '@/domain/runtime/hooks/useFetchRuntimeVersions'
import { Tag } from 'baseui/tag'
import MultiTags from '@/components/Tag/MultiTags'
import { updateRuntimeVersion } from '@/domain/runtime/services/runtimeVersion'

export default function RuntimeVersionListCard() {
    const [page] = usePage()
    const { runtimeId, projectId } = useParams<{ runtimeId: string; projectId: string }>()
    const runtimesInfo = useFetchRuntimeVersions(projectId, runtimeId, page)
    const [t] = useTranslation()

    // @ts-ignore
    // const runtimesInfo = {
    //     isLoading: false,
    //     data: {
    //         list: new Array(100).fill({
    //             // @ts-ignore
    //             meta: '2',
    //             id: '2',
    //             tag: '2',
    //             createdTime: 1000,
    //             // @ts-ignore
    //             owner: null,
    //         }),
    //         pageNum: 1,
    //         pageSize: 1,
    //         total: 1,
    //     },
    // }

    return (
        <Card title={t('runtime versions')}>
            <Table
                isLoading={runtimesInfo.isLoading}
                columns={[
                    t('Meta'),
                    {
                        type: 'tags',
                        title: t('Tag'),
                        minWidth: 200,
                        onAsyncChange: async (value: any, columnIndex: number, rowIndex: number) => {
                            const data = runtimesInfo.data?.list?.[rowIndex]
                            try {
                                await updateRuntimeVersion(projectId, runtimeId, data?.id as string, { tag: value })
                                await runtimesInfo.refetch()
                            } catch (e) {
                                console.error(e)
                            }
                        },
                        mapDataToValue: (item: any) => {
                            // tag index
                            return item[1] ?? ''
                        },
                    },
                    t('Created'),
                    t('Owner'),
                    t('Action'),
                ]}
                data={
                    runtimesInfo.data?.list.map((runtime) => {
                        return [
                            runtime.meta,
                            runtime.tag,
                            runtime.createdTime && formatTimestampDateTime(runtime.createdTime),
                            runtime.owner && <User user={runtime.owner} />,
                            <Button size='mini' key={runtime.id} onClick={() => {}}>
                                {t('Revert')}
                            </Button>,
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
        </Card>
    )
}
