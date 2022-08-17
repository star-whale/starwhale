import React from 'react'
import { Checkbox as BaseCheckbox, CheckboxProps } from 'baseui/checkbox'

export interface ICheckBoxProps extends Omit<CheckboxProps, 'value' | 'onChange'> {
    value?: boolean
    onChange?: (checked: boolean) => void
}

/* eslint-disable react/jsx-props-no-spreading */
export default function Checkbox({ value, onChange, children, ...props }: ICheckBoxProps) {
    return (
        <BaseCheckbox {...props} checked={value} onChange={() => onChange?.(!value)}>
            {children}
        </BaseCheckbox>
    )
}
