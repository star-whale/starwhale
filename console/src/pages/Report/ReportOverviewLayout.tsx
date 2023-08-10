import React from 'react'
import BaseSubLayout from '@/pages/BaseSubLayout'

export default function ReportOverviewLayout({ children }: { children: React.ReactNode }) {
    return <BaseSubLayout breadcrumbItems={[]}>{children}</BaseSubLayout>
}
