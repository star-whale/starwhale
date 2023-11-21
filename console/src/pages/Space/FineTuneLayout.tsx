import useTranslation from '@/hooks/useTranslation'
import React, { useMemo } from 'react'
import { useHistory, useParams } from 'react-router-dom'
import { INavItem } from '@/components/BaseSidebar'
import BaseSubLayout from '@/pages/BaseSubLayout'
import { useAccess } from '@/api/WithAuth'
import Button from '@starwhale/ui/Button'
import { BaseSimpleNavTabs } from '@/components/BaseSimpleNavTabs'
import { useRouterActivePath } from '@/hooks/useRouterActivePath'

export interface IFineTuneLayoutProps {
    children: React.ReactNode
}

export default function FineTuneLayout({ children }: IFineTuneLayoutProps) {
    const { projectId, spaceId } = useParams<{
        projectId: string
        spaceId: any
    }>()
    const history = useHistory()
    const [t] = useTranslation()

    const breadcrumbItems: INavItem[] = useMemo(() => {
        const items = [
            {
                title: t('ft.space.title'),
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
                title: t('ft.eval'),
                path: `/projects/${projectId}/spaces/${spaceId}/fine-tune-evals`,
                pattern: '/\\/fine-tune-evals\\/?',
            },
        ]
        return items
    }, [projectId, spaceId, t])

    const { activeItemId } = useRouterActivePath(navItems)
    const isAccessCreate = useAccess('ft.run.create')
    const isAccessEvalCreate = useAccess('ft.eval.create')

    const actions = [
        {
            access: isAccessCreate && activeItemId === 'fine-tune-runs',
            component: () => (
                <Button
                    size='compact'
                    onClick={() => {
                        history.push(`/projects/${projectId}/new_fine_tune/${spaceId}`)
                    }}
                >
                    {t('create')}
                </Button>
            ),
        },
        {
            access: isAccessEvalCreate && activeItemId === 'fine-tune-evals',
            component: () => (
                <Button
                    size='compact'
                    onClick={() => {
                        history.push(`/projects/${projectId}/new_fine_tune/${spaceId}?type=EVALUATION`)
                    }}
                >
                    {t('create')}
                </Button>
            ),
        },
    ].filter((v) => v.access)

    return (
        <BaseSubLayout
            breadcrumbItems={breadcrumbItems}
            extra={actions.map((v, index) => (
                <v.component key={index} />
            ))}
        >
            <div className='absolute left-1/2 translate-x-[-50%]'>
                <BaseSimpleNavTabs navItems={navItems} />
            </div>

            <div className='content-full h-full'>{children}</div>
        </BaseSubLayout>
    )
}
