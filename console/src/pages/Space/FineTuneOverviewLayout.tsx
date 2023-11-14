import useTranslation from '@/hooks/useTranslation'
import React, { useMemo } from 'react'
import { useHistory, useParams } from 'react-router-dom'
import { INavItem } from '@/components/BaseSidebar'
import BaseSubLayout from '@/pages/BaseSubLayout'
import { BaseNavTabs } from '@/components/BaseNavTabs'
import { usePage } from '@/hooks/usePage'
import { useQueryArgs } from '@/hooks/useQueryArgs'
import { useRouterActivePath } from '@/hooks/useRouterActivePath'
import { useAccess } from '@/api/WithAuth'
import Button from '@starwhale/ui/Button'

export interface IFineTuneLayoutProps {
    children: React.ReactNode
}

export default function FineTuneOverviewLayout({ children }: IFineTuneLayoutProps) {
    const { projectId, fineTuneId, spaceId } = useParams<{
        fineTuneId: string
        projectId: string
        spaceId: any
    }>()
    const { query, updateQuery } = useQueryArgs()
    const [page] = usePage()
    const history = useHistory()
    const [t] = useTranslation()

    const breadcrumbItems: INavItem[] = useMemo(() => {
        const items = [
            {
                title: t('FineTune Spacess'),
                path: `/projects/${projectId}/spaces`,
            },
            {
                title: spaceId,
                path: `/projects/${projectId}/spaces/${spaceId}/fineTunes`,
            },
        ]
        return items
    }, [projectId, spaceId, t])

    const pageParams = useMemo(() => {
        return { ...page, ...query }
    }, [page, query])

    const navItems: INavItem[] = useMemo(() => {
        const items = [
            {
                title: t('Runs'),
                path: `/projects/${projectId}/spaces/${spaceId}/fine-tunes`,
                pattern: '/\\/meta\\/?',
            },
            {
                title: t('online eval'),
                path: `/projects/${projectId}/spaces/${spaceId}/fine-tunes/${fineTuneId}/online-evals`,
                pattern: '/\\/files\\/?',
            },
        ]
        return items
    }, [projectId, fineTuneId, t, pageParams])

    const isAccessCreate = useAccess('Create')

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

    const { activeItemId } = useRouterActivePath(navItems)

    return (
        <BaseSubLayout breadcrumbItems={breadcrumbItems} extra={extra}>
            <div style={{ flex: 1, display: 'flex', flexDirection: 'column' }}>
                <div style={{ marginBottom: '20px' }}>
                    <BaseNavTabs navItems={navItems} />
                </div>
                <div style={{ flex: '1', display: 'flex', flexDirection: 'column' }}>{children}</div>
            </div>
        </BaseSubLayout>
    )
}
