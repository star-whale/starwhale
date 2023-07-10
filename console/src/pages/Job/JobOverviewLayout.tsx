import { useJob, useJobLoading } from '@job/hooks/useJob'
import useTranslation from '@/hooks/useTranslation'
import React, { useEffect, useMemo } from 'react'
import { useQuery } from 'react-query'
import { useParams } from 'react-router-dom'
import { INavItem } from '@/components/BaseSidebar'
import { fetchJob } from '@job/services/job'
import BaseSubLayout from '@/pages/BaseSubLayout'

export interface IJobLayoutProps {
    children: React.ReactNode
}

function JobOverviewLayout({ children }: IJobLayoutProps) {
    const { projectId, jobId } = useParams<{ jobId: string; projectId: string }>()
    const jobInfo = useQuery(`fetchJob:${projectId}:${jobId}`, () => fetchJob(projectId, jobId))
    const { job, setJob } = useJob()
    const { setJobLoading } = useJobLoading()

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
                title: t('Results'),
                path: `/projects/${projectId}/jobs/${jobId}/results`,
            },
            {
                title: t('Tasks'),
                path: `/projects/${projectId}/jobs/${jobId}/tasks`,
                pattern: '/\\/tasks\\/?',
            },
        ]
        return items
    }, [projectId, jobId, t])

    return (
        <BaseSubLayout breadcrumbItems={breadcrumbItems} navItems={navItems}>
            {children}
        </BaseSubLayout>
    )
}

export default React.memo(JobOverviewLayout)
