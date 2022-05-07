import React from 'react'
import { INavItem } from '@/components/BaseSidebar'
import { BaseNavTabs } from '../components/BaseNavTabs'
import BaseLayout from './BaseLayout'
import ProjectSidebar from './Project/ProjectSidebar'

export interface IBaseSubLayoutProps {
    header?: React.ReactNode
    extra?: React.ReactNode
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
