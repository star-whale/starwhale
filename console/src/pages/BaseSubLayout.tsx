import React from 'react'
import { BaseNavTabs } from '../components/BaseNavTabs'
import BaseLayout from './BaseLayout'
import ProjectSidebar from './Project/ProjectSidebar'
import Card from '../components/Card'
import { IComposedSidebarProps, INavItem } from '@/components/BaseSidebar'

export interface IBaseSubLayoutProps {
    header?: React.ReactNode
    extra?: React.ReactNode
    sidebar?: React.ComponentType<IComposedSidebarProps>
    breadcrumbItems?: INavItem[]
    navItems?: INavItem[]
    children: React.ReactNode
}

export default function BaseSubLayout({ header, extra, breadcrumbItems, navItems, children }: IBaseSubLayoutProps) {
    return (
        <BaseLayout extra={extra} breadcrumbItems={breadcrumbItems} sidebar={ProjectSidebar}>
            {header}
            {navItems ? (
                <>
                    <BaseNavTabs navItems={navItems} />
                    <div
                        style={{
                            paddingTop: 15,
                            paddingBottom: 15,
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
