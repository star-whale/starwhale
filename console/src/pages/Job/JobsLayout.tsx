import { useJob, useJobLoading } from '@job/hooks/useJob'
import useTranslation from '@/hooks/useTranslation'
import React, { useEffect, useMemo } from 'react'
import { useQuery } from 'react-query'
import { useParams } from 'react-router-dom'
import { INavItem } from '@/components/BaseSidebar'
import { fetchJob } from '@job/services/job'
import BaseSubLayout from '@/pages/BaseSubLayout'
import { useFetchProject } from '@/domain/project/hooks/useFetchProject'
import ProjectSidebar from '../Project/ProjectSidebar'
import BaseLayout from '../BaseLayout'
import Button from '@/components/Button'

export interface IJobLayoutProps {
    children: React.ReactNode
}

export default function JobsLayout({ children }: IJobLayoutProps) {
    const [t] = useTranslation()
    const header = <></>
    const extra = <></>

    return (
        <BaseLayout
            contentStyle={{
                padding: 0,
                borderRadius: 0,
            }}
            // extra={extra}
            // breadcrumbItems={breadcrumbItems}
            sidebar={ProjectSidebar}
        >
            {header}
            {children}
        </BaseLayout>
    )
}
