import React from 'react'
import { Link } from 'react-router-dom'
import Text from '../Text'
import { useCurrentUser } from '@/hooks/useCurrentUser'
import { createUseStyles } from 'react-jss'

export interface ILogoProps {
    expanded?: boolean
    className?: string
}

export default function Logo({ expanded = true }: ILogoProps) {
    const { currentUser } = useCurrentUser()

    const Logo = expanded ? (
        <Text
            size='large'
            style={{
                display: 'flex',
                fontSize: '30px',
                color: '#fff',
            }}
        >
            StarWhale
        </Text>
    ) : (
        <Text
            size='large'
            style={{
                display: 'flex',
                fontSize: '20px',
                color: '#fff',
            }}
        >
            SW
        </Text>
    )

    if (!currentUser)
        return (
            <div
                style={{
                    flex: `0 0 ${expanded ? 200 : 68}px`,
                    display: 'flex',
                    flexDirection: 'row',
                    textDecoration: 'none',
                    alignItems: 'center',
                    justifyContent: 'center',
                    transition: 'width 200ms cubic-bezier(0.7, 0.1, 0.33, 1) 0ms',
                }}
            >
                {Logo}
            </div>
        )

    return (
        <Link
            style={{
                flex: `0 0 ${expanded ? 200 : 68}px`,
                display: 'flex',
                flexDirection: 'row',
                textDecoration: 'none',
                alignItems: 'center',
                justifyContent: 'center',
                transition: 'width 200ms cubic-bezier(0.7, 0.1, 0.33, 1) 0ms',
            }}
            to='/'
        >
            {Logo}
        </Link>
    )
}
