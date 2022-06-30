import { Input as BaseInput, InputProps, SIZE } from 'baseui/input'
import React from 'react'
import { mergeOverrides } from '@/utils/baseui'

export interface IInputProps extends InputProps {
    overrides?: InputProps['overrides']
    size?: SIZE[keyof SIZE]
}

export default function Input({ size = 'compact', ...props }: IInputProps) {
    const overrides = mergeOverrides(
        {
            Root: {
                style: {
                    ':hover': {
                        borderColor: '#799EE8',
                    },
                },
            },
        },
        props.overrides
    )

    // eslint-disable-next-line  react/jsx-props-no-spreading
    return <BaseInput size={size} {...props} overrides={overrides} />
}
