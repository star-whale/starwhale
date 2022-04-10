import React, { useCallback, useState } from 'react'
import Card from '@/components/Card'
import { usePage } from '@/hooks/usePage'
import { formatTimestampDateTime } from '@/utils/datetime'
import useTranslation from '@/hooks/useTranslation'
import Table from '@/components/Table/index'
import { Link, useParams } from 'react-router-dom'
import { useFetchTasks } from '@job/hooks/useFetchTasks'
import { JobStatusType } from '@/domain/job/schemas/job'
export interface ITaskListCardProps {
    header: React.ReactNode
}

export default function TaskListCard({ header }: ITaskListCardProps) {
    const [page] = usePage()
    const { jobId, projectId } = useParams<{ jobId: string; projectId: string }>()
    const tasksInfo = useFetchTasks(projectId, jobId, page)
    const [t] = useTranslation()

    return (
        <Card>
            {header}
            <Table
                isLoading={tasksInfo.isLoading}
                columns={[t('Task ID'), t('IP'), t('Started'), t('Status')]}
                data={
                    tasksInfo.data?.list.map((task) => {
                        return [
                            task.uuid,
                            task.ip,
                            task.startTime && formatTimestampDateTime(task.startTime),
                            task.status && JobStatusType[task.status],
                        ]
                    }) ?? []
                }
                paginationProps={{
                    start: tasksInfo.data?.pageNum,
                    count: tasksInfo.data?.size,
                    total: tasksInfo.data?.total,
                    afterPageChange: () => {
                        tasksInfo.refetch()
                    },
                }}
            />
        </Card>
    )
}
