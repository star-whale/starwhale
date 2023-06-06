import React from 'react'
import { IComposedSidebarProps, INavItem } from '@/components/BaseSidebar'
import { BaseNavTabs } from '../components/BaseNavTabs'
import BaseLayout from './BaseLayout'
import ProjectSidebar from './Project/ProjectSidebar'
import { useAuth } from '@/api/Auth'

export interface IBaseSubLayoutProps {
    header?: React.ReactNode
    extra?: React.ReactNode
    breadcrumbItems?: INavItem[]
    navItems?: INavItem[]
    children: React.ReactNode
    sidebar?: React.ComponentType<IComposedSidebarProps>
}

export default function BaseSubLayout({
    header,
    extra,
    breadcrumbItems,
    navItems,
    children,
    sidebar,
}: IBaseSubLayoutProps) {
    const { standaloneMode } = useAuth()
    return (
        <BaseLayout
            extra={extra}
            breadcrumbItems={breadcrumbItems}
            sidebar={standaloneMode ? undefined : sidebar ?? ProjectSidebar}
        >
            {header}
            {navItems ? (
                <>
                    <BaseNavTabs navItems={navItems} />
                    <div
                        data-type='sub-layout'
                        style={{
                            paddingTop: 15,
                            paddingBottom: 15,
                            flex: '1',
                            display: 'flex',
                            flexDirection: 'column',
                        }}
                    >
                        {children}
                    </div>
                </>
            ) : (
                <>{children}</>
            )}
        </BaseLayout>
    )
}
