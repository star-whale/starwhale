import { Select, SelectProps, SIZE } from 'baseui/select'
import _ from 'lodash'
import React, { useEffect, useState } from 'react'
import { useQuery } from 'react-query'
import { listDevices } from '../services/system'

export interface IDeviceSelectorProps {
    value?: string
    onChange?: (newValue: string) => void
    overrides?: SelectProps['overrides']
    disabled?: boolean
}

export default function DeviceSelector({ value, onChange, overrides, disabled }: IDeviceSelectorProps) {
    const [keyword, setKeyword] = useState<string>()
    const [options, setOptions] = useState<{ id: string; label: React.ReactNode }[]>([])
    const devicesInfo = useQuery(`listDevices:${keyword}`, () =>
        listDevices({ pageNum: 1, pageSize: 100, search: keyword })
    )

    const handleDeviceInputChange = _.debounce((term: string) => {
        if (!term) {
            setOptions([])
            return
        }
        setKeyword(term)
    })

    useEffect(() => {
        if (devicesInfo.isSuccess) {
            setOptions(
                devicesInfo.data?.map((item) => ({
                    id: item.id,
                    label: item.name,
                })) ?? []
            )
        } else {
            setOptions([])
        }
    }, [devicesInfo.data, devicesInfo.isSuccess])

    return (
        <Select
            size={SIZE.compact}
            disabled={disabled}
            overrides={overrides}
            isLoading={devicesInfo.isFetching}
            options={options}
            onChange={(params) => {
                if (!params.option) {
                    return
                }
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
