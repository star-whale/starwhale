import React, { useCallback, useState } from 'react'
import Card from '@/components/Card'
import { createJob, doJobAction, pinJob } from '@job/services/job'
import { usePage } from '@/hooks/usePage'
import { JobActionType, JobStatusType } from '@job/schemas/job'
import JobForm from '@job/components/JobForm'
import { durationToStr, formatTimestampDateTime } from '@/utils/datetime'
import useTranslation from '@/hooks/useTranslation'
import User from '@/domain/user/components/User'
import { Modal, ModalHeader, ModalBody } from 'baseui/modal'
import Table from '@/components/Table/index'
import { useHistory, useParams } from 'react-router-dom'
import { useFetchJobs } from '@job/hooks/useFetchJobs'
import { toaster } from 'baseui/toast'
import { MonoText } from '@/components/Text'
import JobStatus from '@/domain/job/components/JobStatus'
import Button, { ExtendButton } from '@starwhale/ui/Button'
import { useAccess, useAuthPrivileged, WithCurrentAuth } from '@/api/WithAuth'
import { IconTooltip } from '@starwhale/ui/Tooltip'
import { useProjectRole } from '@project/hooks/useProjectRole'
import { ConfigurationOverride } from '@starwhale/ui/base/helpers/overrides'
import { ConfirmButton, VersionText } from '@starwhale/ui'
import { ExposedButtonLink } from '@job/components/ExposedLink'
import qs from 'qs'
import { IJobRequest, IJobVo } from '@/api'

interface IActionButtonProps {
    // eslint-disable-next-line
    hasText?: boolean
}

export default function JobListCard() {
    const [t] = useTranslation()
    const history = useHistory()
    const [page] = usePage()
    const { projectId } = useParams<{ projectId: string }>()
    const jobsInfo = useFetchJobs(projectId, page)
    const [isCreateJobOpen, setIsCreateJobOpen] = useState(false)
    const { isPrivileged: canPinOrUnpin } = useAuthPrivileged({ role: useProjectRole().role, id: 'job.pinOrUnpin' })

    const handleCreateJob = useCallback(
        async (data: IJobRequest) => {
            await createJob(projectId, data)
            setIsCreateJobOpen(false)
            jobsInfo.refetch()
        },
        [jobsInfo, projectId]
    )
    const handleAction = useCallback(
        async (jobId, type: JobActionType) => {
            await doJobAction(projectId, jobId, type)
            toaster.positive(t('job action done'), { autoHideDuration: 2000 })
            setIsCreateJobOpen(false)
            jobsInfo.refetch()
        },
        [jobsInfo, projectId, t]
    )

    const handlePin = useCallback(
        async (jobId, pinned) => {
            if (!canPinOrUnpin) return
            await pinJob(projectId, jobId, pinned)
            jobsInfo.refetch()
        },
        [canPinOrUnpin, jobsInfo, projectId]
    )

    const isAccessJobCancel = useAccess('job.cancel')
    const isAccessJobPause = useAccess('job.pause')
    const isAccessGlobalJobPause = useAccess('job-pause')
    const isAccessJobResume = useAccess('job.resume')
    const isAccessGlobalJobResume = useAccess('job-resume')

    const CancelButton = (jobId) => ({
        access: isAccessJobCancel,
        component: ({ hasText }: IActionButtonProps) => (
            <ConfirmButton
                isFull
                key='cancel'
                icon='cancel'
                styleas={['menuoption', hasText ? undefined : 'highlight']}
                onClick={() => handleAction(jobId, JobActionType.CANCEL)}
                title={t('Cancel.Confirm')}
            >
                {hasText ? t('Cancel') : undefined}
            </ConfirmButton>
        ),
    })

    const PauseButton = (jobId) => ({
        access: isAccessJobPause && isAccessGlobalJobPause,
        component: ({ hasText }: IActionButtonProps) => (
            <ConfirmButton
                isFull
                key='pause'
                icon='pause'
                styleas={['menuoption', hasText ? undefined : 'highlight']}
                onClick={() => handleAction(jobId, JobActionType.PAUSE)}
                title={t('Pause.Confirm')}
            >
                {hasText ? t('Pause') : undefined}
            </ConfirmButton>
        ),
    })

    const ResumeButton = (jobId) => ({
        access: isAccessJobResume && isAccessGlobalJobResume,
        component: ({ hasText }: IActionButtonProps) => (
            <ExtendButton
                isFull
                key='resume'
                icon='Resume'
                styleas={['menuoption', hasText ? undefined : 'highlight']}
                onClick={() => handleAction(jobId, JobActionType.RESUME)}
            >
                {hasText ? t('Resume') : undefined}
            </ExtendButton>
        ),
    })

    const ViewTaskButton = (jobId) => ({
        access: true,
        component: ({ hasText }: IActionButtonProps) => (
            <ExtendButton
                isFull
                key='ViewTasks'
                icon='a-ViewTasks'
                styleas={['menuoption', hasText ? undefined : 'highlight']}
                onClick={() => history.push(`/projects/${projectId}/jobs/${jobId}/tasks`)}
            >
                {hasText ? t('View Tasks') : undefined}
            </ExtendButton>
        ),
    })

    const getActions = (job: IJobVo) => {
        const statusActions = {
            [JobStatusType.CREATED]: [CancelButton(job.id), PauseButton(job.id)],
            [JobStatusType.RUNNING]: [CancelButton(job.id), PauseButton(job.id)],
            [JobStatusType.PAUSED]: [CancelButton(job.id), ResumeButton(job.id)],
            [JobStatusType.FAIL]: [ResumeButton(job.id)],
            [JobStatusType.SUCCESS]: [ViewTaskButton(job.id)],
        }

        return [
            {
                access: true,
                quickAccess: true,
                component: ({ hasText }) => (
                    <ExtendButton
                        isFull
                        icon='Detail'
                        tooltip={!hasText ? t('View Details') : undefined}
                        styleas={['menuoption', hasText ? undefined : 'highlight']}
                        onClick={() => history.push(`/projects/${projectId}/jobs/${job.id}/overview`)}
                    >
                        {hasText ? t('View Details') : undefined}
                    </ExtendButton>
                ),
            },
            ...(statusActions[job.jobStatus as any] ?? []),
            {
                access: job.jobType !== 'FINE_TUNE',
                component: ({ hasText }) => (
                    <ExtendButton
                        isFull
                        icon='Rerun'
                        styleas={['menuoption', hasText ? undefined : 'highlight']}
                        onClick={() =>
                            history.push(
                                `/projects/${projectId}/new_job?${qs.stringify({
                                    rid: job?.id,
                                })}`
                            )
                        }
                    >
                        {hasText ? t('job.rerun') : undefined}
                    </ExtendButton>
                ),
            },
            ...(job.exposedLinks ?? []).map((exposed) => ({
                access: true,
                component: ({ hasText }) => (
                    <ExposedButtonLink
                        key={exposed.link}
                        data={exposed}
                        hasText
                        styleas={['menuoption', hasText ? undefined : 'highlight']}
                    />
                ),
            })),
        ]
    }

    return (
        <Card
            title={t('Jobs')}
            extra={
                <WithCurrentAuth id='evaluation.create'>
                    <Button
                        onClick={() => {
                            history.push('new_job')
                        }}
                        isLoading={jobsInfo.isLoading}
                    >
                        {t('create')}
                    </Button>
                </WithCurrentAuth>
            }
            style={{
                marginBottom: 0,
            }}
        >
            <Table
                renderActions={(rowIndex) => {
                    const job = jobsInfo.data?.list?.[rowIndex]
                    if (!job) return undefined
                    return getActions(job)
                }}
                isLoading={jobsInfo.isLoading}
                columns={[
                    <div key='jobid' style={{ paddingLeft: '12px' }}>
                        {t('Job ID')}
                    </div>,
                    t('Resource Pool'),
                    t('sth name', [t('Model')]),
                    t('Model Version'),
                    t('Owner'),
                    t('Created'),
                    t('Elapsed Time'),
                    t('End Time'),
                    t('Status'),
                ]}
                data={
                    jobsInfo.data?.list?.map((job) => {
                        const pinBtnStyle: ConfigurationOverride = {
                            position: 'absolute',
                            top: 0,
                            bottom: 0,
                            left: '-8px',
                            display: job.pinnedTime ? 'block' : 'none',
                        }
                        if (canPinOrUnpin) {
                            pinBtnStyle[':hover .iconfont'] = {
                                color: '#FFB23D !important',
                            }
                            pinBtnStyle[':active .iconfont'] = {
                                color: '#F29200  !important',
                            }
                        }

                        return [
                            <div key='id' style={{ gap: '8px', position: 'relative', paddingLeft: '12px' }}>
                                <Button
                                    key='pin'
                                    as='link'
                                    onClick={() => handlePin(job.id, !job.pinnedTime)}
                                    overrides={{
                                        BaseButton: {
                                            props: {
                                                className: 'pin-button',
                                            },
                                            style: pinBtnStyle,
                                        },
                                    }}
                                >
                                    {(canPinOrUnpin || job.pinnedTime) && (
                                        <IconTooltip
                                            content={
                                                // eslint-disable-next-line no-nested-ternary
                                                canPinOrUnpin ? (job.pinnedTime ? t('job.unpin') : t('job.pin')) : null
                                            }
                                            style={{
                                                color: job.pinnedTime ? '#FFB23D' : 'rgba(2,16,43,0.40)',
                                            }}
                                            icon='top'
                                        />
                                    )}
                                </Button>
                                <MonoText>{job.id}</MonoText>
                            </div>,
                            job.resourcePool,
                            job.modelName,
                            <VersionText key='modelVersion' version={job.modelVersion} />,
                            job.owner && <User user={job.owner} />,
                            job?.createdTime && job?.createdTime > 0 && formatTimestampDateTime(job?.createdTime),
                            durationToStr(job.duration),
                            job?.stopTime && job?.stopTime > 0 ? formatTimestampDateTime(job?.stopTime) : '-',
                            <JobStatus key='jobStatus' status={job.jobStatus as any} />,
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
    )
}
