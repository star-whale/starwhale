import { Input as BaseInput, InputProps, SIZE } from 'baseui/input'
import React from 'react'
import { mergeOverrides } from '../utils'
import IconFont from '../IconFont'

export interface IInputProps extends InputProps {
    overrides?: InputProps['overrides']
    size?: keyof typeof SIZE
}

export default function Input({ size = 'compact', ...props }: IInputProps) {
    const overrides = mergeOverrides(
        {
            Root: {
                style: {
                    'borderTopWidth': '1px',
                    'borderBottomWidth': '1px',
                    'borderLeftWidth': '1px',
                    'borderRightWidth': '1px',
                    'paddingLeft': '0px',
                    'paddingRight': '0px',
                    ':hover': {
                        borderColor: '#799EE8',
                    },
                    ':focus': {
                        backgroundColor: '#fff',
                    },
                },
            },
            ClearIcon: {
                style: {
                    'width': '18px',
                    'height': '18px',
                    'fill': 'rgba(2,16,43,0.20)',
                    'color': 'rgba(2,16,43,0.20)',
                    ':hover': {
                        fill: 'rgba(2,16,43,0.40)',
                    },
                },
            },
        },
        props.overrides
    )

    if (props.type === 'password') {
        overrides.MaskToggleShowIcon = () => <IconFont type='eye' kind='gray' />
        overrides.MaskToggleHideIcon = () => <IconFont type='eye_off' kind='gray' />
        overrides.MaskToggleButton = {
            props: { tabIndex: -1 },
        }
    }

    // eslint-disable-next-line  react/jsx-props-no-spreading
    return <BaseInput size={size} {...props} overrides={overrides} />
}
