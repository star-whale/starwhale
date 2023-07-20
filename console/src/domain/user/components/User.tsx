import React from 'react'
import { IUserSchema } from '@user/schemas/user'
import Text, { ITextProps } from '@/components/Text'

export interface IUserProps {
    user?: IUserSchema
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
