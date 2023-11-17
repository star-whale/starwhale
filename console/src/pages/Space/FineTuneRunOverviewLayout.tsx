import useTranslation from '@/hooks/useTranslation'
import React, { useMemo } from 'react'
import { useParams } from 'react-router-dom'
import { INavItem } from '@/components/BaseSidebar'
import BaseSubLayout from '@/pages/BaseSubLayout'
import { BaseNavTabs } from '@/components/BaseNavTabs'

export interface IFineTuneLayoutProps {
    children: React.ReactNode
}

export default function FineTuneRunOverviewLayout({ children }: IFineTuneLayoutProps) {
    const { projectId, fineTuneId, spaceId } = useParams<{
        fineTuneId: string
        projectId: string
        spaceId: any
    }>()
    const [t] = useTranslation()
    const navItems: INavItem[] = useMemo(() => {
        const items = [
            {
                title: t('Overview'),
                path: `/projects/${projectId}/spaces/${spaceId}/fine-tunes/${fineTuneId}/overview`,
                pattern: '/\\/meta\\/?',
            },
            {
                title: t('Tasks'),
                path: `/projects/${projectId}/spaces/${spaceId}/fine-tunes/${fineTuneId}/tasks`,
                pattern: '/\\/tasks\\/?',
            },
        ]
        return items
    }, [projectId, fineTuneId, spaceId, t])

    return (
        <BaseSubLayout>
            <div className='content-full h-full'>
                <div style={{ marginBottom: '20px' }}>
                    <BaseNavTabs navItems={navItems} align='center' />
                </div>
                <div className='content-full h-full'>{children}</div>
            </div>
        </BaseSubLayout>
    )
}
