import React from 'react'
import { useStyletron } from 'baseui'
import bg from '@/assets/bg.jpg'
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
                backgroundColor: 'var(--color-brandLoginBackground)',
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
