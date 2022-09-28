import React from 'react'
import { IComposedSidebarProps, INavItem } from '@/components/BaseSidebar'
import { BaseNavTabs } from '../components/BaseNavTabs'
import BaseLayout from './BaseLayout'
import ProjectSidebar from './Project/ProjectSidebar'

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
    return (
        <BaseLayout extra={extra} breadcrumbItems={breadcrumbItems} sidebar={sidebar ?? ProjectSidebar}>
            {header}
            {navItems ? (
                <>
                    <BaseNavTabs navItems={navItems} />
                    <div
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
