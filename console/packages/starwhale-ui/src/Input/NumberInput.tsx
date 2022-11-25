import { Input, InputProps, SIZE } from 'baseui/input'
import React from 'react'

export interface INumberInputProps {
    value?: number
    onChange?: (value: number) => void
    min?: number
    max?: number
    step?: number
    disabled?: boolean
    type?: 'int' | 'float'
    overrides?: InputProps['overrides']
    size?: SIZE[keyof SIZE]
}

export default function NumberInput({
    value,
    onChange,
    min,
    max,
    step,
    disabled,
    type = 'int',
    overrides,
    size = 'compact',
}: INumberInputProps) {
    const handleChange = (event: React.ChangeEvent<HTMLInputElement>) => {
        const value_ = type === 'float' ? parseFloat(event.target.value) : parseInt(event.target.value, 10)
        if (Number.isNaN(value_)) {
            return
        }
        onChange?.(value_)
    }

    return (
        <Input
            size={size}
            overrides={overrides}
            type='number'
            value={value}
            onChange={handleChange}
            min={min}
            max={max}
            step={step}
            disabled={disabled}
        />
    )
}
