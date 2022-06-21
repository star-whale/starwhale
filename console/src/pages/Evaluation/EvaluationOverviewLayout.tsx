import { useJob, useJobLoading } from '@job/hooks/useJob'
import useTranslation from '@/hooks/useTranslation'
import React, { useContext, useEffect, useMemo } from 'react'
import { useQuery } from 'react-query'
import { useParams } from 'react-router-dom'
import { INavItem } from '@/components/BaseSidebar'
import { fetchJob } from '@job/services/job'
import BaseSubLayout from '@/pages/BaseSubLayout'
import { SidebarContext } from '@/contexts/SidebarContext'
import Card from '@/components/Card'
import { durationToStr, formatTimestampDateTime } from '@/utils/datetime'
import IconFont from '../../components/IconFont/index'

export interface IJobLayoutProps {
    children: React.ReactNode
}

function EvaluationOverviewLayout({ children }: IJobLayoutProps) {
    const { projectId, jobId } = useParams<{ jobId: string; projectId: string }>()
    const jobInfo = useQuery(`fetchJob:${projectId}:${jobId}`, () => fetchJob(projectId, jobId))
    const { job, setJob } = useJob()
    const { setJobLoading } = useJobLoading()
    const { setExpanded } = useContext(SidebarContext)

    // useEffect(() => {
    //     setExpanded(false)
    // }, [setExpanded])

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
                path: `/projects/${projectId}/evaluations`,
            },
            {
                title: uuid,
                path: `/projects/${projectId}/evaluations/${jobId}`,
            },
        ]
        return items
    }, [projectId, jobId, t, uuid])

    const navItems: INavItem[] = useMemo(() => {
        const items = [
            {
                title: t('Results'),
                path: `/projects/${projectId}/evaluations/${jobId}/results`,
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
            label: t('Run time'),
            value: job?.duration && job?.duration > 0 ? durationToStr(job?.duration) : '-',
        },
        {
            label: t('Created time'),
            value: job?.createdTime && formatTimestampDateTime(job.createdTime),
        },
        {
            label: t('End time'),
            value: job?.stopTime && formatTimestampDateTime(job.stopTime),
        },
        {
            label: t('Device'),
            value: `${job?.device ?? '-'}, ${job?.deviceAmount ?? '-'}`,
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
                background: 'var(--color-brandBgSecondory4)',
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
                            background: 'var(--color-brandBgSecondory)',
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

export default React.memo(EvaluationOverviewLayout)
