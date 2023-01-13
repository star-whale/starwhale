import { Select, SelectProps, SIZE } from 'baseui/select'
import _ from 'lodash'
import React, { useState, useEffect } from 'react'

export interface IDeviceSelectorProps {
    data: Array<any>
    value?: string
    onChange?: (newValue: string) => void
    overrides?: SelectProps['overrides']
    disabled?: boolean
}

export default function ResourceSelector({ data, value, onChange, overrides, disabled }: IDeviceSelectorProps) {
    const [keyword, setKeyword] = useState<string>('')
    const [options, setOptions] = useState<{ id: string; label: React.ReactNode }[]>([])

    useEffect(() => {
        setOptions(
            data?.map((item: any) => ({
                id: item.name,
                label: item.name,
            })) ?? []
        )
    }, [data])

    const handleDeviceInputChange = _.debounce((term: string) => {
        setKeyword(term)
    })

    const $options = React.useMemo(() => {
        const _options = options.filter((v) => v.id?.includes(keyword)) ?? []
        const valueExists = options.find((item) => item.id === value)
        if (!valueExists && value)
            _options.push({
                id: value,
                label: value,
            })
        return _options
    }, [options, value, keyword])

    return (
        <Select
            size={SIZE.compact}
            disabled={disabled}
            overrides={overrides}
            options={$options}
            clearable={false}
            creatable
            // @ts-ignore
            ignoreCase={false}
            onChange={(params) => {
                if (!params.option) {
                    return
                }
                setKeyword('')
                onChange?.(params.option.id as string)
            }}
            onInputChange={(e) => {
                const target = e.target as HTMLInputElement
                handleDeviceInputChange(target.value)
            }}
            value={
                value
                    ? [
                          {
                              id: value,
                          },
                      ]
                    : []
            }
        />
    )
}
