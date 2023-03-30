import { Radio as BaseRadio, RadioProps } from 'baseui/radio'
import React from 'react'
import { mergeOverrides } from '../utils'

export interface IRadioProps extends RadioProps {
    overrides?: RadioProps['overrides']
    isFullWidth?: boolean
}

export function Radio({ isFullWidth = false, ...props }: IRadioProps) {
    const overrides = mergeOverrides(
        {
            Root: {
                style: {
                    width: isFullWidth ? '100%' : 'auto',
                },
            },
            Label: {
                style: {
                    width: isFullWidth ? '100%' : 'auto',
                },
            },
            RadioMarkOuter: {
                style: ({ $checked }) => ({
                    'width': '16px',
                    'height': '16px',
                    'backgroundColor': '#FFF',
                    'border': $checked ? '1px solid #2B65D9' : '1px solid #CFD7E6',
                    ':hover': {
                        border: '1px solid #2B65D9',
                    },
                }),
            },
            RadioMarkInner: {
                style: ({ $checked }) => ({
                    width: $checked ? '8px' : '15px',
                    height: $checked ? '8px' : '15px',
                    backgroundColor: $checked ? '#2B65D9' : '#fff',
                }),
            },
        },
        props.overrides
    )

    // eslint-disable-next-line  react/jsx-props-no-spreading
    return <BaseRadio {...props} overrides={overrides} />
}
