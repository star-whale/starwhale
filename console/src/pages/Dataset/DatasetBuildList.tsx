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
import { Modal, ModalHeader, ModalBody } from 'baseui/modal'
import ExecutorForm from '@job/components/ExecutorForm'
import { fetchDatasetBuildList } from '@/domain/dataset/services/dataset'
import { useQuery } from 'react-query'

export interface ITaskListCardProps {
    header: React.ReactNode
    onAction?: (type: string, value: any) => void
}

export default function DatasetBuildList({ header, onAction }: ITaskListCardProps) {
    const [page] = usePage()
    const { jobId, projectId } = useParams<{ jobId: string; projectId: string }>()
    const location = useLocation()
    const id = qs.parse(location.search, { ignoreQueryPrefix: true })?.id ?? ''
    const query = { status: 'BUILDING', ...page }
    const tasksInfo = useQuery(`fetchDatasetBuildList:${projectId}:${qs.stringify(query)}`, () =>
        fetchDatasetBuildList(projectId, query as any)
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
        <Card title={t('dataset.create.title')}>
            {header}
            <Table
                isLoading={tasksInfo.isLoading}
                columns={[
                    t('Task ID'),
                    t('Started'),
                    t('sth name', [t('Dataset')]),
                    t('dataset.create.type'),
                    t('Status'),
                    t('Action'),
                ]}
                data={
                    tasksInfo.data?.list.map((task) => {
                        return [
                            task.id,
                            task.createTime && formatTimestampDateTime(task.createTime),
                            task.datasetName,
                            task.type,
                            task.status,
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
