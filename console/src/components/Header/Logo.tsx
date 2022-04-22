import React from 'react'
import { Link } from 'react-router-dom'
import Text from '../Text'

export interface ILogoProps {
    expanded?: boolean
    className?: string
}

export default function Logo({ expanded = true }: ILogoProps) {
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
            {expanded && (
                <Text
                    size='large'
                    style={{
                        display: 'flex',
                        fontSize: '34px',
                        color: '#fff',
                    }}
                >
                    LOGO
                </Text>
            )}
        </Link>
    )
}
