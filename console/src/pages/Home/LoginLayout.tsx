import React from 'react'
import { useStyletron } from 'baseui'
import BaseLayout from '../BaseLayout'

export interface ILoginLayoutProps {
    children: React.ReactNode
    style?: React.CSSProperties
}

export default function LoginLayout({ children, style }: ILoginLayoutProps) {
    const [, theme] = useStyletron()

    return (
        <BaseLayout
            contentStyle={{
                height: '100%',
            }}
            style={{
                background: 'var(--color-brandLoginBackground)',
                color: theme.colors.contentPrimary,
                ...style,
            }}
        >
            {children}
        </BaseLayout>
    )
}
