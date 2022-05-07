import { Select, SelectProps } from 'baseui/select'
import _ from 'lodash'
import React, { useEffect, useState } from 'react'
import { useQuery } from 'react-query'
import { listBaseImages } from '../services/runtime'

export interface IBaseImageSelectorProps {
    value?: string
    onChange?: (newValue: string) => void
    overrides?: SelectProps['overrides']
    disabled?: boolean
}

export default function BaseImageSelector({ value, onChange, overrides, disabled }: IBaseImageSelectorProps) {
    const [keyword, setKeyword] = useState<string>()
    const [options, setOptions] = useState<{ id: string; label: React.ReactNode }[]>([])
    const baseImagesInfo = useQuery(`listBaseImages:${keyword}`, () =>
        listBaseImages({ pageNum: 1, pageSize: 100, search: keyword })
    )

    const handleBaseImageInputChange = _.debounce((term: string) => {
        if (!term) {
            setOptions([])
            return
        }
        setKeyword(term)
    })

    useEffect(() => {
        if (baseImagesInfo.isSuccess) {
            setOptions(
                baseImagesInfo.data?.map((item) => ({
                    id: item.id,
                    label: item.name,
                })) ?? []
            )
        } else {
            setOptions([])
        }
    }, [baseImagesInfo.data, baseImagesInfo.isSuccess])

    return (
        <Select
            disabled={disabled}
            overrides={overrides}
            isLoading={baseImagesInfo.isFetching}
            options={options}
            onChange={(params) => {
                if (!params.option) {
                    return
                }
                onChange?.(params.option.id as string)
            }}
            onInputChange={(e) => {
                const target = e.target as HTMLInputElement
                handleBaseImageInputChange(target.value)
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
