import useTranslation from '@/hooks/useTranslation'
import React, { useMemo } from 'react'
import { useHistory, useParams } from 'react-router-dom'
import { INavItem } from '@/components/BaseSidebar'
import BaseSubLayout from '@/pages/BaseSubLayout'
import { useAccess } from '@/api/WithAuth'
import Button from '@starwhale/ui/Button'
import { BaseSimpleNavTabs } from '@/components/BaseSimpleNavTabs'

export interface IFineTuneLayoutProps {
    children: React.ReactNode
}

export default function FineTuneLayout({ children }: IFineTuneLayoutProps) {
    const { projectId, fineTuneId, spaceId } = useParams<{
        fineTuneId: string
        projectId: string
        spaceId: any
    }>()
    const history = useHistory()
    const [t] = useTranslation()

    const breadcrumbItems: INavItem[] = useMemo(() => {
        const items = [
            {
                title: t('fine-tuning'),
                path: `/projects/${projectId}/spaces`,
            },
            {
                title: spaceId,
                path: `/projects/${projectId}/spaces/${spaceId}/fine-tunes`,
            },
        ]
        return items
    }, [projectId, spaceId, t])

    const navItems: INavItem[] = useMemo(() => {
        const items = [
            {
                title: t('ft.runs'),
                path: `/projects/${projectId}/spaces/${spaceId}/fine-tune-runs`,
                pattern: '/\\/fine-tune-runs\\/?',
            },
            {
                title: t('ft.online_eval'),
                path: `/projects/${projectId}/spaces/${spaceId}/fine-tune-evals`,
                pattern: '/\\/fine-tune-evals\\/?',
            },
        ]
        return items
    }, [projectId, spaceId, t])

    const isAccessCreate = useAccess('ft.run.create')

    const extra = (
        <Button
            size='compact'
            onClick={() => {
                history.push(`/projects/${projectId}/new_fine_tune/${spaceId}`)
            }}
        >
            {t('create')}
        </Button>
    )

    return (
        <BaseSubLayout breadcrumbItems={breadcrumbItems} extra={isAccessCreate && extra}>
            <div className='absolute left-1/2 translate-x-[-50%]'>
                <BaseSimpleNavTabs navItems={navItems} />
            </div>

            <div className='content-full h-full'>{children}</div>
        </BaseSubLayout>
    )
}
