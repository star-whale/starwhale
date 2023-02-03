import React from 'react'
import { CheckboxProps } from 'baseui/checkbox'
import Checkbox from './Checkbox'

export interface IFormCheckboxProps extends Omit<CheckboxProps, 'value' | 'onChange'> {
    value?: boolean
    onChange?: (checked: boolean) => void
}

/* eslint-disable react/jsx-props-no-spreading */
export function FormCheckbox({ value, onChange, children, ...props }: IFormCheckboxProps) {
    return (
        <Checkbox {...props} checked={value} onChange={() => onChange?.(!value)}>
            {children}
        </Checkbox>
    )
}
