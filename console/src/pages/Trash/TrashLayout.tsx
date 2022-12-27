import React from 'react'
import BaseSubLayout from '@/pages/BaseSubLayout'

export interface ITrashLayoutProps {
    children: React.ReactNode
}

export default function TrashLayout({ children }: ITrashLayoutProps) {
    return <BaseSubLayout>{children}</BaseSubLayout>
}
