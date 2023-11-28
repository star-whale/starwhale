import useTranslation from '@/hooks/useTranslation'
import React, { useMemo, useReducer } from 'react'
import { useHistory, useParams } from 'react-router-dom'
import { INavItem } from '@/components/BaseSidebar'
import BaseSubLayout from '@/pages/BaseSubLayout'
import { useAccess } from '@/api/WithAuth'
import Button from '@starwhale/ui/Button'
import { BaseSimpleNavTabs } from '@/components/BaseSimpleNavTabs'
import { useRouterActivePath } from '@/hooks/useRouterActivePath'
import { EvalSelectListModal } from '@/domain/space/components/EvalSelectList'
import useFineTuneEvaluation from '@/domain/space/hooks/useFineTuneEvaluation'
import { useEventCallback } from '@starwhale/core'

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
    const [key, forceUpdate] = useReducer((s) => s + 1, 0)

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
            {
                title: t('ft.eval.online'),
                path: `/projects/${projectId}/spaces/${spaceId}/fine-tune-onlines`,
                pattern: '/\\/fine-tune-onlines\\/?',
            },
        ]
        return items
    }, [projectId, spaceId, t])

    const { activeItemId } = useRouterActivePath(navItems)
    const isAccessCreate = useAccess('ft.run.create')
    const isAccessEvalCreate = useAccess('ft.eval.create')
    const isAccessEvalExport = useAccess('ft.eval.export')
    const isAccessEvalImport = useAccess('ft.eval.import')
    const [isExport, setIsExport] = React.useState(false)
    const [isImport, setIsImport] = React.useState(false)

    const config = useFineTuneEvaluation()

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
            access: isAccessEvalImport && activeItemId === 'fine-tune-evals',
            component: () => (
                <Button size='compact' kind='secondary' onClick={() => setIsImport(true)}>
                    {t('ft.eval.import')}
                </Button>
            ),
        },
        {
            access: isAccessEvalExport && activeItemId === 'fine-tune-evals',
            component: () => (
                <Button size='compact' kind='secondary' onClick={() => setIsExport(true)}>
                    {t('ft.eval.export')}
                </Button>
            ),
        },
        {
            access: isAccessEvalCreate && activeItemId === 'fine-tune-evals',
            component: () => (
                <Button
                    size='compact'
                    onClick={() => {
                        history.push(`/projects/${projectId}/new_fine_tune_eval/${spaceId}`)
                    }}
                >
                    {t('create')}
                </Button>
            ),
        },
    ].filter((v) => v.access)

    const submitExport = useEventCallback((ids) => {
        config.exportEval(ids)
        forceUpdate()
    })
    const submitImport = useEventCallback((ids) => {
        config.importEval(ids)
        forceUpdate()
    })

    return (
        <BaseSubLayout
            breadcrumbItems={breadcrumbItems}
            extra={
                <div className='flex gap-10px'>
                    {actions.map((v, index) => (
                        <v.component key={index} />
                    ))}
                </div>
            }
        >
            <div className='absolute left-1/2 translate-x-[-50%]'>
                <BaseSimpleNavTabs navItems={navItems} />
            </div>
            <div className='content-full h-full' key={key}>
                {children}
            </div>
            <EvalSelectListModal isOpen={isExport} setIsOpen={setIsExport} type='export' onSubmit={submitExport} />
            <EvalSelectListModal isOpen={isImport} setIsOpen={setIsImport} type='import' onSubmit={submitImport} />
        </BaseSubLayout>
    )
}
