/* eslint-disable react/no-unused-prop-types */
import React from 'react'
import { STYLE_TYPE, CheckboxOverrides } from 'baseui/checkbox'
import { mergeOverrides } from '../utils'
import Checkbox from '../Checkbox'

export interface IToggleProps {
    value?: boolean
    onChange?: (newView: boolean) => void
    overrides?: CheckboxOverrides
    disabled?: boolean
    style?: React.CSSProperties
}

export function Toggle({ value, onChange, disabled, ...props }: IToggleProps) {
    const overrides = mergeOverrides(
        {
            Root: {
                style: {
                    width: '37px',
                },
            },

            Toggle: {
                style: ({ $checked }) => {
                    if (disabled) {
                        return {
                            backgroundColor: $checked ? '  rgba(255,255,255,0.60)' : 'rgba(255,255,255,0.40);',
                            width: '12px',
                            height: '12px',
                        }
                    }

                    return {
                        backgroundColor: '#FFF',
                        width: '12px',
                        height: '12px',
                    }
                },
            },
            ToggleTrack: {
                style: ({ $checked }: any) => {
                    if (disabled) {
                        return {
                            backgroundColor: $checked ? ' rgba(43,101,217,0.30)' : ' rgba(2,16,43,0.20)',
                            width: '39px',
                            paddingLeft: '1px',
                        }
                    }
                    return {
                        backgroundColor: $checked ? '#2B65D9' : 'rgba(2,16,43,0.20)',
                        width: '39px',
                        paddingLeft: '2px',
                    }
                },
            },
        },
        props.overrides
    )

    return (
        <Checkbox
            disabled={disabled}
            checked={value}
            overrides={overrides}
            checkmarkType={STYLE_TYPE.toggle_round}
            onChange={(e) => {
                // eslint-disable-next-line @typescript-eslint/no-explicit-any
                onChange?.((e.target as any).checked)
            }}
            // eslint-disable-next-line  react/jsx-props-no-spreading
            {...props}
        />
    )
}
