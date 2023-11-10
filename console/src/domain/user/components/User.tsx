import React from 'react'
import Text, { ITextProps } from '@/components/Text'
import { IUserVo } from '@/api'

export interface IUserProps {
    user?: IUserVo
    style?: React.CSSProperties
    size?: ITextProps['size']
}

export default function User({ user, style, size = 'medium' }: IUserProps) {
    const { name } = user || {}

    return (
        <div
            style={{
                display: 'flex',
                alignItems: 'center',
                gap: 10,
                ...style,
            }}
        >
            <Text size={size}>{name}</Text>
        </div>
    )
}
