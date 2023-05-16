import React from 'react'
import { WidgetProps } from '@rjsf/utils'
import Input from '../../../Input'

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
            // @ts-ignore
            onBlur={!readonly ? handleBlur : undefined}
            // @ts-ignore
            onChange={!readonly ? handleChange : undefined}
            // @ts-ignore
            onFocus={!readonly ? handleFocus : undefined}
            placeholder={placeholder}
            rows={options.rows || 4}
            value={value}
        />
    )
}

export default TextWidget
