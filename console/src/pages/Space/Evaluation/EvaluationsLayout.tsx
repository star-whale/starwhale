import React from 'react'
import ProjectSidebar from '../../Project/ProjectSidebar'
import BaseLayout from '../../BaseLayout'

export interface IJobLayoutProps {
    children: React.ReactNode
}

export default function EvaluationsLayout({ children }: IJobLayoutProps) {
    const header = <></>

    return (
        <BaseLayout
            contentStyle={{
                padding: 0,
                borderRadius: 0,
            }}
            sidebar={ProjectSidebar}
        >
            {header}
            {children}
        </BaseLayout>
    )
}
