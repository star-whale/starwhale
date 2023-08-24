import React from 'react'
import { WidgetProps } from '@rjsf/utils'
import Select, { SIZE } from '../../../Select'
import IconFont from '@starwhale/ui/IconFont'

const SELECT_STYLE = {
    width: '100%',
    minWidth: '280px',
}

const SelectWidget = ({
    autofocus,
    disabled,
    formContext,
    id,
    multiple,
    onBlur,
    onChange,
    onFocus,
    options,
    placeholder,
    readonly,
    value,
}: WidgetProps) => {
    const { readonlyAsDisabled = true } = formContext

    // eslint-disable-next-line @typescript-eslint/no-unused-vars
    const { enumOptions, enumDisabled } = options

    const handleChange = (nextValue: any) => {
        return !multiple
            ? onChange(nextValue?.option?.id as string)
            : onChange(
                  nextValue.value?.map((item: any) => (item.id as string) ?? '').filter((name: string) => name !== '')
              )
    }

    const handleBlur = () => onBlur(id, value)

    const handleFocus = () => onFocus(id, value)

    const stringify = (currentValue: any) =>
        Array.isArray(currentValue) ? value.map((v: any) => ({ id: String(v) })) : [{ id: String(value) }]

    // Antd's typescript definitions do not contain the following props that are actually necessary and, if provided,
    // they are used, so hacking them in via by spreading `extraProps` on the component to avoid typescript errors
    const extraProps = {
        name: id,
    }
    const $options = React.useMemo(() => {
        if (!Array.isArray(enumOptions)) return []
        return enumOptions.map(({ value: optionValue, label: optionLabel }) => {
            return {
                id: optionValue,
                label: optionLabel,
            }
        })
    }, [enumOptions])

    const $value =
        multiple && Array.isArray(value)
            ? value?.map((item) => ({
                  id: item,
                  label: item,
              }))
            : stringify(value)

    return (
        <Select
            multi={multiple}
            closeOnSelect={!multiple}
            maxDropdownHeight='200px'
            overrides={{
                ControlContainer: {
                    style: {
                        ...SELECT_STYLE,
                    },
                },
                Tag: {
                    props: {
                        overrides: {
                            Root: {
                                style: {
                                    color: 'rgba(2, 16, 43)',
                                    backgroundColor: 'rgb(238, 241, 246)',
                                },
                            },
                            ActionIcon: () => <IconFont type='close' size={12} kind='gray' />,
                        },
                    },
                },
            }}
            size={SIZE.compact}
            autoFocus={autofocus}
            disabled={disabled || (readonlyAsDisabled && readonly)}
            id={id}
            onBlur={!readonly ? handleBlur : undefined}
            onChange={!readonly ? handleChange : undefined}
            onFocus={!readonly ? handleFocus : undefined}
            placeholder={placeholder}
            value={$value}
            options={$options}
            {...extraProps}
        />
    )
}

SelectWidget.defaultProps = {
    formContext: {},
}

export default SelectWidget
