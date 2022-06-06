import React from 'react'
import Card from '@/components/Card'
import { usePage } from '@/hooks/usePage'
import { formatTimestampDateTime } from '@/utils/datetime'
import useTranslation from '@/hooks/useTranslation'
import { Button } from 'baseui/button'
import User from '@/domain/user/components/User'
import Table from '@/components/Table'
import { useParams } from 'react-router-dom'
import { useFetchRuntimeVersions } from '@/domain/runtime/hooks/useFetchRuntimeVersions'

export default function RuntimeVersionListCard() {
    const [page] = usePage()
    const { runtimeId, projectId } = useParams<{ runtimeId: string; projectId: string }>()
    const runtimesInfo = useFetchRuntimeVersions(projectId, runtimeId, page)
    const [t] = useTranslation()

    return (
        <Card title={t('runtime versions')}>
            <Table
                isLoading={runtimesInfo.isLoading}
                columns={[t('Meta'), t('Created'), t('Owner'), t('Action')]}
                data={
                    runtimesInfo.data?.list.map((runtime) => {
                        return [
                            runtime.meta,
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
