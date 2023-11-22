import { useJob, useJobLoading } from '@job/hooks/useJob'
import useTranslation from '@/hooks/useTranslation'
import React, { useEffect, useMemo } from 'react'
import { useQuery } from 'react-query'
import { useParams } from 'react-router-dom'
import { INavItem } from '@/components/BaseSidebar'
import { fetchJob } from '@job/services/job'
import BaseSubLayout from '@/pages/BaseSubLayout'
import { useAuth } from '@/api/Auth'
import { BaseNavTabs } from '@/components/BaseNavTabs'

export interface IJobLayoutProps {
    children: React.ReactNode
}

function FineTuneEvaluationOverviewLayout({ children }: IJobLayoutProps) {
    const { projectId, jobId, spaceId } = useParams<{ jobId: string; projectId: string; spaceId: any }>()
    const jobInfo = useQuery(`fetchJob:${projectId}:${jobId}`, () => fetchJob(projectId, jobId))
    const { job, setJob } = useJob()
    const { setJobLoading } = useJobLoading()
    const { standaloneMode } = useAuth()

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
                path: `/projects/${projectId}/spaces/${spaceId}/evaluations`,
            },
            {
                title: job?.uuid ?? '-',
                path: `/projects/${projectId}/spaces/${spaceId}/evaluations/${jobId}`,
            },
        ]
        return standaloneMode ? [] : items
    }, [projectId, jobId, t, job, standaloneMode, spaceId])

    const navItems: INavItem[] = useMemo(() => {
        const items = [
            {
                title: t('Overview'),
                path: `/projects/${projectId}/spaces/${spaceId}/evaluations/${jobId}/overview`,
            },
            {
                title: t('Results'),
                path: `/projects/${projectId}/spaces/${spaceId}/evaluations/${jobId}/results`,
            },
            {
                title: t('Tasks'),
                path: `/projects/${projectId}/spaces/${spaceId}/evaluations/${jobId}/tasks`,
            },
        ]
        return standaloneMode ? [] : items
    }, [projectId, jobId, t, standaloneMode, spaceId])

    return (
        <BaseSubLayout breadcrumbItems={breadcrumbItems}>
            <div className='content-full h-full'>
                <div style={{ marginBottom: '20px' }}>
                    <BaseNavTabs navItems={navItems} align='center' />
                </div>
                <div className='content-full h-full overflow-auto'>{children}</div>
            </div>
        </BaseSubLayout>
    )
}

export default FineTuneEvaluationOverviewLayout
