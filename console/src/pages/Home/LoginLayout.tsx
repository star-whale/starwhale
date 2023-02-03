import React from 'react'
import bg from '@/assets/bg.jpg'
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
            style={{
                backgroundColor: theme.brandLoginBackground,
                backgroundImage: `url(${bg})`,
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
