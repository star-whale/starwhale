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
import Button, { ButtonGroup, ExtendButton } from '@starwhale/ui/Button'
import { Modal, ModalHeader, ModalBody } from 'baseui/modal'
import ExecutorForm from '@job/components/ExecutorForm'
import Text from '@starwhale/ui/Text'
import ExposedLink from '@job/components/ExposedLink'

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
    const [currentTaskExecutor, setCurrentTaskExecutor] = React.useState<string>('')

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
                    t('Status Desc'),
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
                            <Text key='statusDesc' tooltip={task.failedReason} content={task.failedReason} />,
                            <ButtonGroup key='action'>
                                <ExtendButton
                                    tooltip={t('View Log')}
                                    key={task.uuid}
                                    as='link'
                                    icon='a-Viewlog'
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
                                />
                                <WithCurrentAuth id='job-dev' key='exposed'>
                                    {task.exposedLinks?.map((exposed) => (
                                        <ExposedLink key={exposed.link} data={exposed} />
                                    ))}
                                </WithCurrentAuth>
                                <WithCurrentAuth id='task.execute'>
                                    {task.taskStatus === TaskStatusType.RUNNING && (
                                        <ExtendButton
                                            tooltip={t('job.task.executor')}
                                            key={task.uuid}
                                            as='link'
                                            icon='docker'
                                            onClick={() => setCurrentTaskExecutor(task.id)}
                                        />
                                    )}
                                </WithCurrentAuth>
                            </ButtonGroup>,
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

            <Modal
                isOpen={currentTaskExecutor !== ''}
                onClose={() => setCurrentTaskExecutor('')}
                overrides={{
                    Dialog: {
                        style: {
                            width: '50vw',
                            height: '50vh',
                            display: 'flex',
                            flexDirection: 'column',
                        },
                    },
                }}
                closeable
                animate
                autoFocus
            >
                <ModalHeader>{t('job.task.executor')}</ModalHeader>
                <ModalBody>
                    <ExecutorForm project={projectId} job={jobId} task={currentTaskExecutor} />
                </ModalBody>
            </Modal>
        </Card>
    )
}
