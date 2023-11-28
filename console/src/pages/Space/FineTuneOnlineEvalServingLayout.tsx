import React from 'react'
import BaseSubLayout from '@/pages/BaseSubLayout'

export interface ILayoutProps {
    children: React.ReactNode
}

export default function FineTuneOnlineEvalServingLayout({ children }: ILayoutProps) {
    return <BaseSubLayout>{children}</BaseSubLayout>
}
