import React, { useCallback, useState, memo } from 'react'
import './Runs.scss'
import TableGrid from '@/components/Table/TableGrid'
import { Column } from '@/components/BaseTable'
import Card from '@/components/Card'
import { createJob } from '@job/services/job'
import { ICreateJobSchema } from '@job/schemas/job'
import JobForm from '@job/components/JobForm'
import { durationToStr, formatTimestampDateTime } from '@/utils/datetime'
import useTranslation from '@/hooks/useTranslation'
import { Button, SIZE as ButtonSize } from 'baseui/button'
import User from '@/domain/user/components/User'
import { Modal, ModalHeader, ModalBody } from 'baseui/modal'
import { Link, useHistory, useParams } from 'react-router-dom'
import { useFetchJobs } from '@job/hooks/useFetchJobs'

function JobGridCard() {
    const { projectId } = useParams<{ jobId: string; projectId: string }>()
    const jobsInfo = useFetchJobs(projectId, { pageNum: 1, pageSize: 99999 })
    const [isCreateJobOpen, setIsCreateJobOpen] = useState(false)
    const handleCreateJob = useCallback(
        async (data: ICreateJobSchema) => {
            await createJob(projectId, data)
            await jobsInfo.refetch()
            setIsCreateJobOpen(false)
        },
        [jobsInfo, projectId]
    )
    // const handleAction = useCallback(
    //     async (jobId, type: JobActionType) => {
    //         await doJobAction(projectId, jobId, type)
    //         toaster.positive(t('job action done'), { autoHideDuration: 2000 })
    //         await jobsInfo.refetch()
    //         setIsCreateJobOpen(false)
    //     },
    //     [jobsInfo]
    // )
    const history = useHistory()
    const [t] = useTranslation()

    const columns = [
        {
            key: 'uuid',
            title: 'uuid',
            dataKey: 'uuid',
            resizable: true,
            width: 280,
            align: Column.Alignment.CENTER,
            frozen: Column.FrozenDirection.LEFT,
            cellRenderer: ({ rowData }: any) => (
                <Link key={rowData.id} to={`/projects/${projectId}/jobs/${rowData.id}`}>
                    {rowData.uuid}
                </Link>
            ),
        },
        {
            key: 'modelName',
            title: 'modelName',
            dataKey: 'modelName',
            resizable: true,
            sortable: true,
            width: 150,
            align: Column.Alignment.CENTER,
        },
        {
            key: 'modelVersion',
            title: 'modelVersion',
            dataKey: 'modelVersion',
            resizable: true,
            sortable: true,
            width: 150,
            align: Column.Alignment.CENTER,
        },
        {
            key: 'owner',
            title: 'owner',
            width: 150,
            align: Column.Alignment.CENTER,
            cellRenderer: ({ rowData }: any) => <User user={rowData.owner} />,
        },
        {
            key: 'createTime',
            title: 'createTime',
            resizable: true,
            sortable: true,
            width: 150,
            align: Column.Alignment.CENTER,
            cellRenderer: ({ rowData }: any) => rowData.createTime && formatTimestampDateTime(rowData.createTime),
        },
        {
            key: 'duration',
            title: 'duration',
            width: 150,
            align: Column.Alignment.CENTER,
            cellRenderer: ({ rowData }: any) =>
                typeof rowData.duration === 'string' ? '-' : durationToStr(rowData.duration),
        },
        {
            key: 'stopTime',
            title: 'stopTime',
            width: 150,
            align: Column.Alignment.CENTER,
            cellRenderer: ({ rowData }: any) =>
                rowData.stopTime > 0 ? formatTimestampDateTime(rowData.stopTime) : '-',
        },
        // {
        //     key: 'action',
        //     width: 50,
        //     align: Column.Alignment.CENTER,
        //     frozen: Column.FrozenDirection.RIGHT
        //     cellRenderer: ({ rowData}: any) => {
        //         const job = rowData
        //         const actions: Partial<Record<JobStatusType, React.ReactNode>> = {
        //             [JobStatusType.preparing]: (
        //                 <StyledLink onClick={() => handleAction(job.id, 'cancel')}>{t('Cancel')}</StyledLink>
        //             ),
        //             [JobStatusType.runnning]: (
        //                 <StyledLink onClick={() => handleAction(job.id, 'cancel')}>{t('Cancel')}</StyledLink>
        //             ),
        //             [JobStatusType.completed]: (
        //                 <Link to={`/projects/${projectId}/jobs/${job.id}/results`}>{t('View Results')}</Link>
        //             ),
        //         }
        //         return actions[job.jobStatus] ?? ''
        //     },
        // },
    ]

    return (
        <Card
            title={t('Jobs')}
            extra={
                <Button
                    size={ButtonSize.compact}
                    onClick={() => {
                        history.push('new_job')
                    }}
                    isLoading={jobsInfo.isLoading}
                >
                    {t('create')}
                </Button>
            }
            bodyStyle={{
                width: '100%',
                boxSizing: 'border-box',
                height: 'calc(100vh - 234px)',
            }}
        >
            <TableGrid
                data={jobsInfo.data?.list || []}
                columns={columns}
                tableConfigMap={{
                    enableMultiSelection: true,
                }}
            />
            <Modal isOpen={isCreateJobOpen} onClose={() => setIsCreateJobOpen(false)} closeable animate autoFocus>
                <ModalHeader>{t('create sth', [t('Job')])}</ModalHeader>
                <ModalBody>
                    <JobForm onSubmit={handleCreateJob} />
                </ModalBody>
            </Modal>
        </Card>
    )
}

export default memo(JobGridCard)
