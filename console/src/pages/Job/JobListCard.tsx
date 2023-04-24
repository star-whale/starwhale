import React, { useCallback, useState } from 'react'
import Card from '@/components/Card'
import { createJob, doJobAction } from '@job/services/job'
import { usePage } from '@/hooks/usePage'
import { ICreateJobSchema, JobActionType, JobStatusType } from '@job/schemas/job'
import JobForm from '@job/components/JobForm'
import { durationToStr, formatTimestampDateTime } from '@/utils/datetime'
import useTranslation from '@/hooks/useTranslation'
import User from '@/domain/user/components/User'
import { Modal, ModalHeader, ModalBody } from 'baseui/modal'
import Table from '@/components/Table/index'
import { useHistory, useParams } from 'react-router-dom'
import { useFetchJobs } from '@job/hooks/useFetchJobs'
import { toaster } from 'baseui/toast'
import { TextLink } from '@/components/Link'
import { MonoText } from '@/components/Text'
import JobStatus from '@/domain/job/components/JobStatus'
import Button from '@starwhale/ui/Button'

export default function JobListCard() {
    const [t] = useTranslation()
    const history = useHistory()
    const [page] = usePage()
    const { projectId } = useParams<{ projectId: string }>()
    const jobsInfo = useFetchJobs(projectId, page)
    const [isCreateJobOpen, setIsCreateJobOpen] = useState(false)
    const handleCreateJob = useCallback(
        async (data: ICreateJobSchema) => {
            await createJob(projectId, data)
            await jobsInfo.refetch()
            setIsCreateJobOpen(false)
        },
        [jobsInfo, projectId]
    )
    const handleAction = useCallback(
        async (jobId, type: JobActionType) => {
            await doJobAction(projectId, jobId, type)
            toaster.positive(t('job action done'), { autoHideDuration: 2000 })
            await jobsInfo.refetch()
            setIsCreateJobOpen(false)
        },
        [jobsInfo, projectId, t]
    )

    return (
        <>
            <Card
                title={t('Jobs')}
                extra={
                    <Button
                        onClick={() => {
                            history.push('new_job')
                        }}
                        isLoading={jobsInfo.isLoading}
                    >
                        {t('create')}
                    </Button>
                }
            >
                <Table
                    isLoading={jobsInfo.isLoading}
                    columns={[
                        t('Job ID'),
                        t('Resource Pool'),
                        t('sth name', [t('Model')]),
                        t('Version'),
                        t('Owner'),
                        t('Created'),
                        t('Elapsed Time'),
                        t('End Time'),
                        t('Status'),
                        t('Action'),
                    ]}
                    data={
                        jobsInfo.data?.list.map((job) => {
                            const actions: Partial<Record<JobStatusType, React.ReactNode>> = {
                                [JobStatusType.CREATED]: (
                                    <>
                                        <Button
                                            kind='tertiary'
                                            onClick={() => handleAction(job.id, JobActionType.CANCEL)}
                                        >
                                            {t('Cancel')}
                                        </Button>
                                        &nbsp;&nbsp;
                                        <Button
                                            kind='tertiary'
                                            onClick={() => handleAction(job.id, JobActionType.PAUSE)}
                                        >
                                            {t('Pause')}
                                        </Button>
                                    </>
                                ),
                                [JobStatusType.RUNNING]: (
                                    <>
                                        <Button
                                            kind='tertiary'
                                            onClick={() => handleAction(job.id, JobActionType.CANCEL)}
                                        >
                                            {t('Cancel')}
                                        </Button>
                                        &nbsp;&nbsp;
                                        <Button
                                            kind='tertiary'
                                            onClick={() => handleAction(job.id, JobActionType.PAUSE)}
                                        >
                                            {t('Pause')}
                                        </Button>
                                    </>
                                ),
                                [JobStatusType.PAUSED]: (
                                    <>
                                        <Button
                                            kind='tertiary'
                                            onClick={() => handleAction(job.id, JobActionType.CANCEL)}
                                        >
                                            {t('Cancel')}
                                        </Button>
                                        &nbsp;&nbsp;
                                        <Button
                                            kind='tertiary'
                                            onClick={() => handleAction(job.id, JobActionType.RESUME)}
                                        >
                                            {t('Resume')}
                                        </Button>
                                    </>
                                ),
                                [JobStatusType.FAIL]: (
                                    <>
                                        <Button
                                            kind='tertiary'
                                            onClick={() => handleAction(job.id, JobActionType.RESUME)}
                                        >
                                            {t('Resume')}
                                        </Button>
                                    </>
                                ),
                                [JobStatusType.SUCCESS]: (
                                    <Button
                                        kind='tertiary'
                                        onClick={() => history.push(`/projects/${projectId}/jobs/${job.id}/tasks`)}
                                    >
                                        {t('View Tasks')}
                                    </Button>
                                ),
                            }

                            return [
                                <TextLink key={job.id} to={`/projects/${projectId}/jobs/${job.id}/actions`}>
                                    <MonoText>{job.uuid}</MonoText>
                                </TextLink>,
                                job.resourcePool,
                                job.modelName,
                                <MonoText key='modelVersion'>{job.modelVersion}</MonoText>,
                                job.owner && <User user={job.owner} />,
                                job?.createdTime && job?.createdTime && formatTimestampDateTime(job?.createdTime),
                                typeof job.duration === 'string' ? '-' : durationToStr(job.duration),
                                job?.stopTime && job?.stopTime > 0 ? formatTimestampDateTime(job?.stopTime) : '-',
                                <JobStatus key='jobStatus' status={job.jobStatus as any} />,
                                actions[job.jobStatus] ?? '',
                            ]
                        }) ?? []
                    }
                    paginationProps={{
                        start: jobsInfo.data?.pageNum,
                        count: jobsInfo.data?.pageSize,
                        total: jobsInfo.data?.total,
                        afterPageChange: () => {
                            jobsInfo.refetch()
                        },
                    }}
                />
                <Modal isOpen={isCreateJobOpen} onClose={() => setIsCreateJobOpen(false)} closeable animate autoFocus>
                    <ModalHeader>{t('create sth', [t('Job')])}</ModalHeader>
                    <ModalBody>
                        <JobForm onSubmit={handleCreateJob} />
                    </ModalBody>
                </Modal>
            </Card>
        </>
    )
}
