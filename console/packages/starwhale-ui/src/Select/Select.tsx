import { Select as BaseSelect, SelectProps, SIZE } from 'baseui/select'
import React from 'react'
import { mergeOverrides } from '../utils'

export interface ISelectProps extends SelectProps {
    overrides?: SelectProps['overrides']
    size?: keyof typeof SIZE
}

export default function Select({ size = 'compact', ...props }: ISelectProps) {
    const overrides = mergeOverrides(
        {
            ControlContainer: {
                style: {
                    'borderTopWidth': '1px',
                    'borderBottomWidth': '1px',
                    'borderLeftWidth': '1px',
                    'borderRightWidth': '1px',
                    ':hover': {
                        borderColor: '#799EE8',
                    },
                },
            },
        },
        props.overrides
    )

    // eslint-disable-next-line  react/jsx-props-no-spreading
    return <BaseSelect size={size} {...props} overrides={overrides} />
}
