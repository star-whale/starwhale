import React from 'react'
import BaseLayout from '../BaseLayout'
import { themedUseStyletron } from '@starwhale/ui/theme/styletron'

export interface ILoginLayoutProps {
    children: React.ReactNode
    style?: React.CSSProperties
}

export default function LoginLayout({ children, style }: ILoginLayoutProps) {
    const [, theme] = themedUseStyletron()

    return (
        <BaseLayout
            contentStyle={{
                height: '100%',
            }}
            className='login-bg'
            style={{
                backgroundColor: theme.brandLoginBackground,
                backgroundSize: 'cover',
                backgroundPosition: 'center',
                backgroundRepeat: 'no-repeat',
                color: theme.colors.contentPrimary,
                justifyContent: 'center',
                ...style,
            }}
        >
            {children}
        </BaseLayout>
    )
}
