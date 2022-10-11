import React from 'react'
import BaseSubLayout from '../BaseSubLayout'
import SettingsSidebar from './SettingsSidebar'

export interface IJobLayoutProps {
    children: React.ReactNode
}

function SettingsOverviewLayout({ children }: IJobLayoutProps) {
    return (
        <BaseSubLayout breadcrumbItems={[]} sidebar={SettingsSidebar}>
            {children}
        </BaseSubLayout>
    )
}

export default React.memo(SettingsOverviewLayout)
