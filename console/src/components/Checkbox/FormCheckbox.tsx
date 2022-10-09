import React from 'react'
import { CheckboxProps } from 'baseui/checkbox'
import Checkbox from './Checkbox'

export interface ICheckBoxProps extends Omit<CheckboxProps, 'value' | 'onChange'> {
    value?: boolean
    onChange?: (checked: boolean) => void
}

/* eslint-disable react/jsx-props-no-spreading */
export default function FormCheckbox({ value, onChange, children, ...props }: ICheckBoxProps) {
    return (
        <Checkbox {...props} checked={value} onChange={() => onChange?.(!value)}>
            {children}
        </Checkbox>
    )
}
