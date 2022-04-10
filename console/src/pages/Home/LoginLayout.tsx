import React from 'react'
import BaseLayout from '../BaseLayout'

export interface ILoginLayoutProps {
    children: React.ReactNode
    style?: React.CSSProperties
}

export default function LoginLayout({ children, style }: ILoginLayoutProps) {
    return <BaseLayout style={style}>{children}</BaseLayout>
}
