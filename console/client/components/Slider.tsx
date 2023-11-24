import React, { useCallback, useState } from 'react'

export interface ISliderProps {
    label: string
    description?: string
    defaultValue?: number
    step?: number
    max?: number
    min?: number
    onChange: (value: number) => void
}

export const Slider = ({
    label,
    description,
    defaultValue = 0,
    step = 0.1,
    max = 1,
    min = 0,
    onChange,
}: ISliderProps) => {
    const [value, setValue] = useState(defaultValue)
    const handleChange = (event: React.ChangeEvent<HTMLInputElement>) => {
        const newValue = parseFloat(event.target.value)
        setValue(newValue)
        onChange(newValue)
    }

    const formatValue = useCallback(
        (v: number) => {
            const stepStr = step.toString()
            const stepDecimal = stepStr.split('.')[1]
            const stepDecimalLength = stepDecimal ? stepDecimal.length : 0
            return Number.isInteger(v) ? v : v.toFixed(stepDecimalLength)
        },
        [step]
    )

    return (
        <div className='flex flex-col w-100% mb-8'>
            <label className='mb-2 text-left text-neutral-700 dark:text-neutral-400'>{label}</label>
            <span className='text-[12px] text-black/50 dark:text-white/50 text-sm'>{description}</span>
            <span className='mt-2 mb-1 text-center text-neutral-900 dark:text-neutral-100'>{formatValue(value)}</span>
            <input
                className='cursor-pointer'
                type='range'
                min={min}
                max={max}
                step={step}
                value={value}
                onChange={handleChange}
            />
        </div>
    )
}
