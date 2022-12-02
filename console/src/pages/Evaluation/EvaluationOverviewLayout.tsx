import { useJob, useJobLoading } from '@job/hooks/useJob'
import useTranslation from '@/hooks/useTranslation'
import React, { useEffect, useMemo } from 'react'
import { useQuery } from 'react-query'
import { useParams } from 'react-router-dom'
import { INavItem } from '@/components/BaseSidebar'
import { doJobAction, fetchJob } from '@job/services/job'
import BaseSubLayout from '@/pages/BaseSubLayout'
import { durationToStr, formatTimestampDateTime } from '@/utils/datetime'
import Accordion from '@/components/Accordion'
import { Panel } from 'baseui/accordion'
import { JobActionType, JobStatusType } from '@/domain/job/schemas/job'
import { toaster } from 'baseui/toast'
import Button from '@/components/Button'
import { WithCurrentAuth } from '@/api/WithAuth'
import { StatefulTooltip } from 'baseui/tooltip'

export interface IJobLayoutProps {
    children: React.ReactNode
}

function EvaluationOverviewLayout({ children }: IJobLayoutProps) {
    const { projectId, jobId } = useParams<{ jobId: string; projectId: string }>()
    const jobInfo = useQuery(`fetchJob:${projectId}:${jobId}`, () => fetchJob(projectId, jobId))
    const { job, setJob } = useJob()
    const { setJobLoading } = useJobLoading()

    useEffect(() => {
        setJobLoading(jobInfo.isLoading)
        if (jobInfo.isSuccess) {
            setJob(jobInfo.data)
        } else if (jobInfo.isLoading) {
            setJob(undefined)
        }
    }, [job?.id, jobInfo.data, jobInfo.isLoading, jobInfo.isSuccess, setJob, setJobLoading])

    const [t] = useTranslation()

    const breadcrumbItems: INavItem[] = useMemo(() => {
        const items = [
            {
                title: t('Evaluations'),
                path: `/projects/${projectId}/evaluations`,
            },
            {
                title: job?.uuid ?? '-',
                path: `/projects/${projectId}/evaluations/${jobId}`,
            },
        ]
        return items
    }, [projectId, jobId, t, job])

    const navItems: INavItem[] = useMemo(() => {
        const items = [
            {
                title: t('Results'),
                path: `/projects/${projectId}/evaluations/${jobId}/results`,
            },
            {
                title: t('DAG'),
                path: `/projects/${projectId}/evaluations/${jobId}/actions`,
            },
            {
                title: t('Tasks'),
                path: `/projects/${projectId}/evaluations/${jobId}/tasks`,
                pattern: '/\\/tasks\\/?',
            },
        ]
        return items
    }, [projectId, jobId, t])

    const info = React.useMemo(() => {
        const items = [
            {
                label: t('Owner'),
                value: job?.owner?.name ?? '-',
            },
            {
                label: t('Status'),
                value: job?.jobStatus ?? '-',
            },
            {
                label: t('Elapsed Time'),
                value: job?.duration && job?.duration > 0 ? durationToStr(job?.duration) : '-',
            },
            {
                label: t('Created'),
                value: job?.createdTime && formatTimestampDateTime(job.createdTime),
            },
            {
                label: t('End Time'),
                value: job?.stopTime && formatTimestampDateTime(job.stopTime),
            },
            {
                label: t('Model'),
                style: {
                    gridColumnStart: 'span 2',
                },
                value: `${job?.modelName ?? '-'} : ${job?.modelVersion ?? '-'}`,
            },
            {
                label: t('Datasets'),
                style: {
                    gridColumnStart: 'span 2',
                },
                value: job?.datasets?.join(', '),
            },
            {
                label: t('Runtime'),
                style: {
                    gridColumnStart: 'span 2',
                },
                value: (
                    <StatefulTooltip content={() => <pre>{job?.runtime?.version?.meta}</pre>} placement='bottomRight'>
                        {[
                            job?.runtime?.name ?? '-',
                            job?.runtime?.version?.alias,
                            job?.runtime?.version?.name ?? '-',
                        ].join(' : ')}
                    </StatefulTooltip>
                ),
            },
            {
                label: t('Image'),
                style: {
                    gridColumnStart: 'span 2',
                },
                value: job?.runtime?.version?.image ?? '-',
            },
        ]

        return (
            <div
                style={{
                    fontSize: '14px',
                    display: 'grid',
                    gridTemplateColumns: 'repeat(auto-fit, minmax(380px, 1fr))',
                    gap: '12px',
                }}
            >
                {items.map((v) => (
                    <div key={v?.label} style={{ display: 'flex', gap: '12px' }}>
                        <div
                            style={{
                                lineHeight: '24px',
                                borderRadius: '4px',
                                color: 'rgba(2,16,43,0.60)',
                            }}
                        >
                            {v?.label}:
                        </div>
                        <div> {v?.value}</div>
                    </div>
                ))}
            </div>
        )
    }, [job, t])

    const header = useMemo(
        () => (
            <div className='mb-20'>
                <Accordion accordion>
                    <Panel title={`${t('Evaluation ID')}: ${job?.id ?? ''}`}>{info}</Panel>
                </Accordion>
            </div>
        ),
        [job, info, t]
    )
    const handleAction = React.useCallback(
        async (jobIdArg, type: JobActionType) => {
            await doJobAction(projectId, jobIdArg, type)
            toaster.positive(t('job action done'), { autoHideDuration: 2000 })
            await jobInfo.refetch()
        },
        [jobInfo, projectId, t]
    )

    const extra = React.useMemo(() => {
        if (!job) return <></>

        const actions: Partial<Record<JobStatusType, React.ReactNode>> = {
            [JobStatusType.CREATED]: (
                <>
                    <Button onClick={() => handleAction(job.id, JobActionType.CANCEL)}>{t('Cancel')}</Button>
                    &nbsp;&nbsp;
                    <Button onClick={() => handleAction(job.id, JobActionType.PAUSE)}>{t('Pause')}</Button>
                </>
            ),
            [JobStatusType.RUNNING]: (
                <>
                    <Button onClick={() => handleAction(job.id, JobActionType.CANCEL)}>{t('Cancel')}</Button>
                    &nbsp;&nbsp;
                    <Button onClick={() => handleAction(job.id, JobActionType.PAUSE)}>{t('Pause')}</Button>
                </>
            ),
            [JobStatusType.PAUSED]: (
                <>
                    <Button onClick={() => handleAction(job.id, JobActionType.CANCEL)}>{t('Cancel')}</Button>
                    &nbsp;&nbsp;
                    <Button onClick={() => handleAction(job.id, JobActionType.RESUME)}>{t('Resume')}</Button>
                </>
            ),
            [JobStatusType.FAIL]: (
                <>
                    <Button onClick={() => handleAction(job.id, JobActionType.RESUME)}>{t('Resume')}</Button>
                </>
            ),
        }

        return <WithCurrentAuth id='evaluation.action'>{actions[job.jobStatus]}</WithCurrentAuth>
    }, [job, t, handleAction])

    return (
        <BaseSubLayout header={header} breadcrumbItems={breadcrumbItems} navItems={navItems} extra={extra}>
            <div style={{ paddingTop: '12px', flex: '1', display: 'flex', flexDirection: 'column' }}>{children}</div>
        </BaseSubLayout>
    )
}

export default React.memo(EvaluationOverviewLayout)
