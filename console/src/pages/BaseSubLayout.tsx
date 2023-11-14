import React from 'react'
import { IComposedSidebarProps, INavItem } from '@/components/BaseSidebar'
import { BaseNavTabs } from '../components/BaseNavTabs'
import BaseLayout from './BaseLayout'
import ProjectSidebar from './Project/ProjectSidebar'
import { useAuth } from '@/api/Auth'
import { useRouteInlineContext } from '@/contexts/RouteInlineContext'

export interface IBaseSubLayoutProps {
    header?: React.ReactNode
    extra?: React.ReactNode
    breadcrumbItems?: INavItem[]
    navItems?: INavItem[]
    children: React.ReactNode
    sidebar?: React.ComponentType<IComposedSidebarProps>
    contentStyle?: React.CSSProperties
}

export default function BaseSubLayout({
    header,
    extra,
    breadcrumbItems,
    navItems,
    children,
    sidebar,
    contentStyle,
}: IBaseSubLayoutProps) {
    const { standaloneMode } = useAuth()

    const { isInline } = useRouteInlineContext()

    if (isInline) return children

    return (
        <BaseLayout
            extra={extra}
            breadcrumbItems={breadcrumbItems}
            sidebar={standaloneMode ? undefined : sidebar ?? ProjectSidebar}
            contentStyle={contentStyle}
        >
            {header}
            {navItems ? (
                <>
                    <BaseNavTabs navItems={navItems} />
                    <div data-type='sub-layout' className='flex flex-1 flex-col overflow-hidden py-15px'>
                        <div className='flex flex-1 flex-col overflow-auto'>{children}</div>
                    </div>
                </>
            ) : (
                <>{children}</>
            )}
        </BaseLayout>
    )
}
