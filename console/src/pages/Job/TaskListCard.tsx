import React, { useEffect } from 'react'
import Card from '@/components/Card'
import { usePage } from '@/hooks/usePage'
import { formatTimestampDateTime } from '@/utils/datetime'
import useTranslation from '@/hooks/useTranslation'
import Table from '@/components/Table/index'
import { useParams, useLocation } from 'react-router-dom'
import { useFetchTasks } from '@job/hooks/useFetchTasks'
import { StyledLink } from 'baseui/link'
import _ from 'lodash'
import qs from 'qs'
import moment from 'moment'
import JobStatus from '@/domain/job/components/JobStatus'
import { WithCurrentAuth } from '@/api/WithAuth'
import { IconFont } from '@starwhale/ui'
import { TaskStatusType } from '@/domain/job/schemas/task'

export interface ITaskListCardProps {
    header: React.ReactNode
    onAction?: (type: string, value: any) => void
}

export default function TaskListCard({ header, onAction }: ITaskListCardProps) {
    const [page] = usePage()
    const { jobId, projectId } = useParams<{ jobId: string; projectId: string }>()
    const location = useLocation()
    const id = qs.parse(location.search, { ignoreQueryPrefix: true })?.id ?? ''

    const tasksInfo = useFetchTasks(projectId, jobId, page)
    const [t] = useTranslation()

    useEffect(() => {
        if (id && tasksInfo.data?.list) {
            const taskInfo = tasksInfo.data?.list.find((task) => task.id === id)
            if (taskInfo)
                onAction?.('viewlog', {
                    ...taskInfo,
                })
        }
    }, [tasksInfo.isSuccess, tasksInfo.data, id, onAction])

    return (
        <Card>
            {header}
            <Table
                isLoading={tasksInfo.isLoading}
                columns={[
                    t('Task ID'),
                    t('Step'),
                    t('Resource Pool'),
                    t('Started'),
                    t('End Time'),
                    t('Duration'),
                    t('Status'),
                    t('Action'),
                ]}
                data={
                    tasksInfo.data?.list.map((task) => {
                        return [
                            task.id,
                            task.stepName,
                            task.resourcePool,
                            task.startedTime && formatTimestampDateTime(task.startedTime),
                            task.finishedTime && formatTimestampDateTime(task.finishedTime),
                            task.finishedTime && task.startedTime && task.finishedTime !== -1 && task.startedTime !== -1
                                ? moment.duration(task.finishedTime - task.startedTime, 'milliseconds').humanize()
                                : '-',
                            <JobStatus key='status' status={task.taskStatus as any} />,
                            <p key='action' style={{ display: 'flex', gap: '10px' }}>
                                <StyledLink
                                    key={task.uuid}
                                    onClick={(e: any) => {
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
                                </StyledLink>
                                <WithCurrentAuth id='job-dev' key='devUrl'>
                                    {(bool: boolean) =>
                                        bool && task.devUrl && task.taskStatus === TaskStatusType.RUNNING ? (
                                            <a target='_blank' href={task.devUrl} rel='noreferrer' title='debug'>
                                                <IconFont type='vscode' size={14} />
                                            </a>
                                        ) : (
                                            ''
                                        )
                                    }
                                </WithCurrentAuth>
                            </p>,
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
