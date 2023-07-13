import React, { useEffect } from 'react'
import Card from '@/components/Card'
import { usePage } from '@/hooks/usePage'
import { formatTimestampDateTime } from '@/utils/datetime'
import useTranslation from '@/hooks/useTranslation'
import Table from '@/components/Table/index'
import { useParams, useLocation } from 'react-router-dom'
import { StyledLink } from 'baseui/link'
import _ from 'lodash'
import qs from 'qs'
import moment from 'moment'
import JobStatus from '@/domain/job/components/JobStatus'
import { WithCurrentAuth } from '@/api/WithAuth'
import { IconFont } from '@starwhale/ui'
import { TaskStatusType } from '@/domain/job/schemas/task'
import Button from '@starwhale/ui/Button'
import { Modal, ModalHeader, ModalBody } from 'baseui/modal'
import ExecutorForm from '@job/components/ExecutorForm'
import Text from '@starwhale/ui/Text'
import { fetchDatasetBuildList } from '@/domain/dataset/services/dataset'
import { useQuery } from 'react-query'

export interface ITaskListCardProps {
    header: React.ReactNode
    onAction?: (type: string, value: any) => void
}

export default function DatasetTaskBuildList({ header, onAction }: ITaskListCardProps) {
    const [page] = usePage()
    const { jobId, projectId } = useParams<{ jobId: string; projectId: string }>()
    const location = useLocation()
    const id = qs.parse(location.search, { ignoreQueryPrefix: true })?.id ?? ''
    const query = { status: 'FAILED', ...page }
    const tasksInfo = useQuery(`fetchDatasetBuildList:${projectId}:${qs.stringify(query)}`, () =>
        fetchDatasetBuildList(projectId, query)
    )

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
        <Card title='1'>
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
                            task.datasetName,
                            task.datasetId,
                            task.type,
                            task.status,
                            task.createTime && formatTimestampDateTime(task.createTime),
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
                                <WithCurrentAuth id='task.execute'>
                                    {task.taskStatus === TaskStatusType.RUNNING && (
                                        <Button onClick={() => setCurrentTaskExecutor(task.id)} as='link'>
                                            <IconFont type='docker' size={14} />
                                        </Button>
                                    )}
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
