import React from 'react'
import Card from '@/components/Card'
import { usePage } from '@/hooks/usePage'
import { formatTimestampDateTime } from '@/utils/datetime'
import useTranslation from '@/hooks/useTranslation'
import Table from '@/components/Table/index'
import { useParams } from 'react-router-dom'
import { useFetchTasks } from '@job/hooks/useFetchTasks'
import { StyledLink } from 'baseui/link'
import _ from 'lodash'

export interface ITaskListCardProps {
    header: React.ReactNode
    onAction?: (type: string, value: any) => void
}

export default function TaskListCard({ header, onAction }: ITaskListCardProps) {
    const [page] = usePage()
    const { jobId, projectId } = useParams<{ jobId: string; projectId: string }>()
    const tasksInfo = useFetchTasks(projectId, jobId, page)
    const [t] = useTranslation()

    return (
        <Card>
            {header}
            <Table
                isLoading={tasksInfo.isLoading}
                columns={[t('Task ID'), t('IP'), t('Version'), t('Started'), t('Status'), t('Action')]}
                data={
                    tasksInfo.data?.list.map((task) => {
                        return [
                            task.uuid,
                            task.agent?.ip,
                            task.agent?.version,
                            task.createdTime && formatTimestampDateTime(task.createdTime),
                            task.taskStatus,
                            <StyledLink
                                key={task.uuid}
                                onClick={(e) => {
                                    // eslint-disalbe-next-line no-unused-expressions
                                    const trDom = e.currentTarget.closest('tr')
                                    const trDoms = trDom?.parentElement?.children
                                    _.forEach(trDoms, (d) => {
                                        d?.classList.remove('tr--selected')
                                    })
                                    trDom?.classList.add('tr--selected')

                                    onAction?.('viewlog', {
                                        ...task,
                                    })
                                }}
                            >
                                {t('View Log')}
                            </StyledLink>,
                        ]
                    }) ?? []
                }
                paginationProps={{
                    start: tasksInfo.data?.pageNum,
                    count: tasksInfo.data?.pageSize,
                    total: tasksInfo.data?.total,
                    afterPageChange: () => {
                        tasksInfo.refetch()
                    },
                }}
            />
        </Card>
    )
}
