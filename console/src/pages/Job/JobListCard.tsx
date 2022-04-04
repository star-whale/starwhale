import React, { useCallback, useState } from 'react'
import Card from '@/components/Card'
import { createJob, doJobAction } from '@job/services/job'
import { usePage } from '@/hooks/usePage'
import { ICreateJobSchema, JobActionType, JobStatusType } from '@job/schemas/job'
import JobForm from '@job/components/JobForm'
import { durationToStr, formatTimestampDateTime } from '@/utils/datetime'
import useTranslation from '@/hooks/useTranslation'
import { Button, SIZE as ButtonSize } from 'baseui/button'
import User from '@/components/User'
import { Modal, ModalHeader, ModalBody } from 'baseui/modal'
import Table from '@/components/Table'
import { Link, useHistory, useParams } from 'react-router-dom'
import { useFetchJobs } from '@job/hooks/useFetchJobs'
import { resourceIconMapping } from '@/consts'
import { StyledLink } from 'baseui/link'
import { toaster } from 'baseui/toast'

export default function JobListCard() {
    const [page] = usePage()
    const { jobId, projectId } = useParams<{ jobId: string; projectId: string }>()

    const jobsInfo = useFetchJobs(projectId, page)
    const [isCreateJobOpen, setIsCreateJobOpen] = useState(false)
    const handleCreateJob = useCallback(
        async (data: ICreateJobSchema) => {
            await createJob(projectId, data)
            await jobsInfo.refetch()
            setIsCreateJobOpen(false)
        },
        [jobsInfo]
    )
    const handleAction = useCallback(
        async (jobId, type: JobActionType) => {
            await doJobAction(projectId, jobId, type)
            toaster.positive(t('job action done'), { autoHideDuration: 2000 })
            await jobsInfo.refetch()
            setIsCreateJobOpen(false)
        },
        [jobsInfo]
    )
    const [t] = useTranslation()
    const history = useHistory()

    return (
        <Card
            title={t('jobs')}
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
        >
            <Table
                isLoading={jobsInfo.isLoading}
                columns={[
                    t('Job ID'),
                    t('sth name', [t('Model')]),
                    t('Version'),
                    t('Owner'),
                    t('Created time'),
                    t('Run time'),
                    t('End time'),
                    t('Status'),
                    t('Action'),
                ]}
                data={
                    jobsInfo.data?.list.map((job) => {
                        const actions: Partial<Record<JobStatusType, React.ReactNode>> = {
                            [JobStatusType.preparing]: (
                                <StyledLink onClick={() => handleAction(job.id, 'cancel')}>{t('Cancel')}</StyledLink>
                            ),
                            [JobStatusType.runnning]: (
                                <StyledLink onClick={() => handleAction(job.id, 'cancel')}>{t('Cancel')}</StyledLink>
                            ),
                            [JobStatusType.completed]: (
                                <Link to={`/projects/${projectId}/jobs/${job.id}`}>{t('View Results')}</Link>
                            ),
                        }

                        return [
                            <Link key={job.id} to={`/projects/${projectId}/jobs/${job.id}`}>
                                {job.uuid}
                            </Link>,
                            job.modelName,
                            job.modelVersion,
                            job.owner && <User user={job.owner} />,
                            job.createTime && formatTimestampDateTime(job.createTime),
                            typeof job.duration == 'string' ? '-' : durationToStr(job.duration),
                            job.stopTime > 0 ? formatTimestampDateTime(job.stopTime) : '-',
                            job.jobStatus && JobStatusType[job.jobStatus],
                            actions[job.jobStatus] ?? '',
                        ]
                    }) ?? []
                }
                paginationProps={{
                    start: jobsInfo.data?.pageNum,
                    count: jobsInfo.data?.size,
                    total: jobsInfo.data?.total,
                    afterPageChange: () => {
                        jobsInfo.refetch()
                    },
                }}
            />
            <Modal
                isOpen={isCreateJobOpen}
                onClose={() => setIsCreateJobOpen(false)}
                closeable
                animate
                autoFocus
                unstable_ModalBackdropScroll
            >
                <ModalHeader>{t('create sth', [t('Job')])}</ModalHeader>
                <ModalBody>
                    <JobForm onSubmit={handleCreateJob} />
                </ModalBody>
            </Modal>
        </Card>
    )
}
