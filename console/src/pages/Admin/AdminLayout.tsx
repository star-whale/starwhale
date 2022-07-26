import React from 'react'
import BaseSubLayout from '@/pages/BaseSubLayout'
import AdminSidebar from '@/pages/Admin/AdminSidebar'

export interface IAdminLayoutProps {
    children: React.ReactNode
}

export default function AdminLayout({ children }: IAdminLayoutProps) {
    return <BaseSubLayout sidebar={AdminSidebar}>{children}</BaseSubLayout>
}
