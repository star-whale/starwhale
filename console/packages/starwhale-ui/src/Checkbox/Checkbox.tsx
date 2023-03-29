import React from 'react'
import { Checkbox as BaseCheckbox, CheckboxProps } from 'baseui/checkbox'
import { mergeOverrides } from '../utils/baseui'
import { expandBorder, expandBorderRadius } from '../utils'

export interface ICheckBoxProps extends CheckboxProps {
    size?: number
}

/* eslint-disable react/jsx-props-no-spreading */
export default function Checkbox({ size = 16, children, ...props }: ICheckBoxProps) {
    const overrides = mergeOverrides(
        {
            Root: {
                style: {
                    alignItems: 'center',
                    minHeight: '16px',
                },
            },
            Checkmark: {
                style: ({ $checked }: any) => {
                    return {
                        'width': `${size}px`,
                        'height': `${size}px`,
                        'backgroundSize': '80%',
                        'backgroundPosition': 'center',
                        ...expandBorder($checked ? '0px' : '1px', 'solid', '#CFD7E6'),
                        ...expandBorderRadius('2px'),
                        ':hover': {
                            ...expandBorder('', '', '#799EE8'),
                        },
                    }
                },
            },
        },
        props.overrides
    )

    return (
        <BaseCheckbox {...props} overrides={overrides}>
            {children}
        </BaseCheckbox>
    )
}
