import React from 'react'
import { WidgetProps } from '@rjsf/utils'
import Input from '@/components/Input'

const INPUT_STYLE = {
    width: '100%',
}

const TextWidget = ({
    disabled,
    formContext,
    id,
    onBlur,
    onChange,
    onFocus,
    options,
    placeholder,
    readonly,
    value,
}: WidgetProps) => {
    const { readonlyAsDisabled = true } = formContext

    const handleChange = ({ target }: React.ChangeEvent<HTMLInputElement>) =>
        onChange(target.value === '' ? options.emptyValue : target.value)

    const handleBlur = ({ target }: React.FocusEvent<HTMLInputElement>) => onBlur(id, target.value)

    const handleFocus = ({ target }: React.FocusEvent<HTMLInputElement>) => onFocus(id, target.value)

    return (
        <Input
            disabled={disabled || (readonlyAsDisabled && readonly)}
            id={id}
            name={id}
            onBlur={!readonly ? handleBlur : undefined}
            onChange={!readonly ? handleChange : undefined}
            onFocus={!readonly ? handleFocus : undefined}
            placeholder={placeholder}
            rows={options.rows || 4}
            style={INPUT_STYLE}
            value={value}
        />
    )
}

export default TextWidget
