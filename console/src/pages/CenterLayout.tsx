import React from 'react'
import { useStyletron } from 'baseui'
import BaseLayout from './BaseLayout'

export interface ILoginLayoutProps {
    children: React.ReactNode
    style?: React.CSSProperties
}

export default function CenterLayout({ children, style }: ILoginLayoutProps) {
    const [, theme] = useStyletron()

    return (
        <BaseLayout
            sidebar={() => <></>}
            contentStyle={{
                height: '100%',
                maxWidth: '1400px',
                width: '100%',
                alignSelf: 'center',
                overflow: 'initial',
            }}
            style={{
                color: theme.colors.contentPrimary,
                ...style,
            }}
        >
            {children}
        </BaseLayout>
    )
}
