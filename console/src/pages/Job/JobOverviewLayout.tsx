import { useJob, useJobLoading } from '@job/hooks/useJob'
import useTranslation from '@/hooks/useTranslation'
import React, { useEffect, useMemo } from 'react'
import { useQuery } from 'react-query'
import { useParams } from 'react-router-dom'
import { INavItem } from '@/components/BaseSidebar'
import { fetchJob } from '@job/services/job'
import BaseSubLayout from '@/pages/BaseSubLayout'
import Card from '@/components/Card'
import { durationToStr, formatTimestampDateTime } from '@/utils/datetime'
import IconFont from '@starwhale/ui/IconFont'
import { themedUseStyletron } from '@starwhale/ui/theme/styletron'

export interface IJobLayoutProps {
    children: React.ReactNode
}

function JobOverviewLayout({ children }: IJobLayoutProps) {
    const { projectId, jobId } = useParams<{ jobId: string; projectId: string }>()
    const jobInfo = useQuery(`fetchJob:${projectId}:${jobId}`, () => fetchJob(projectId, jobId))
    const { job, setJob } = useJob()
    const { setJobLoading } = useJobLoading()
    const [, theme] = themedUseStyletron()

    useEffect(() => {
        setJobLoading(jobInfo.isLoading)
        if (jobInfo.isSuccess) {
            if (jobInfo.data.id !== job?.id) {
                setJob(jobInfo.data)
            }
        } else if (jobInfo.isLoading) {
            setJob(undefined)
        }
    }, [job?.id, jobInfo.data, jobInfo.isLoading, jobInfo.isSuccess, setJob, setJobLoading])

    const [t] = useTranslation()
    const uuid = job?.uuid ?? '-'

    const breadcrumbItems: INavItem[] = useMemo(() => {
        const items = [
            {
                title: t('Jobs'),
                path: `/projects/${projectId}/jobs`,
            },
            {
                title: uuid,
                path: `/projects/${projectId}/jobs/${jobId}`,
            },
        ]
        return items
    }, [projectId, jobId, t, uuid])

    const navItems: INavItem[] = useMemo(() => {
        const items = [
            {
                title: t('DAG'),
                path: `/projects/${projectId}/jobs/${jobId}/actions`,
                icon: <IconFont type='results' />,
            },
            {
                title: t('Tasks'),
                path: `/projects/${projectId}/jobs/${jobId}/tasks`,
                pattern: '/\\/tasks\\/?',
                icon: <IconFont type='tasks' />,
            },
            {
                title: t('Results'),
                path: `/projects/${projectId}/jobs/${jobId}/results`,
                icon: <IconFont type='results' />,
            },
        ]
        return items
    }, [projectId, jobId, t])

    const items = [
        {
            label: t('Job ID'),
            value: job?.id ?? '-',
        },
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
            value: `${job?.modelName ?? '-'}:${job?.modelVersion ?? '-'}`,
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
            value: [job?.runtime?.name ?? '-', job?.runtime?.version?.name ?? '-'].join(':'),
        },
    ]

    const header = (
        <Card
            style={{
                fontSize: '16px',
                padding: '12px 20px',
                marginBottom: '10px',
            }}
            bodyStyle={{
                display: 'grid',
                gridTemplateColumns: 'repeat(auto-fit, minmax(400px, 1fr))',
                gap: '12px',
            }}
        >
            {items.map((v) => (
                <div key={v?.label} style={{ display: 'flex', gap: '12px', ...v.style }}>
                    <div
                        style={{
                            // flexBasis: '130px',
                            background: theme.brandBgSecondary,
                            lineHeight: '24px',
                            padding: '0 12px',
                            borderRadius: '4px',
                        }}
                    >
                        {v?.label}:
                    </div>
                    <div> {v?.value}</div>
                </div>
            ))}
        </Card>
    )
    return (
        <BaseSubLayout header={header} breadcrumbItems={breadcrumbItems} navItems={navItems}>
            {children}
        </BaseSubLayout>
    )
}

export default React.memo(JobOverviewLayout)
