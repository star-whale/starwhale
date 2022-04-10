import React from 'react'
import { IUserSchema } from '@user/schemas/user'
import Text from '@/components/Text'

export interface IUserProps {
    user: IUserSchema
    style?: React.CSSProperties
}

export default function User({ user, style }: IUserProps) {
    const name = user.name

    return (
        <div
            style={{
                display: 'flex',
                alignItems: 'center',
                gap: 10,
                ...style,
            }}
        >
            <Text>{name}</Text>
        </div>
    )
}
