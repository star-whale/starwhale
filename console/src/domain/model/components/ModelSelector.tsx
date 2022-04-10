import { listModels } from '../services/model'
import { Select, SelectProps } from 'baseui/select'
import _ from 'lodash'
import React, { useEffect, useState } from 'react'
import { useQuery } from 'react-query'
import { useParams } from 'react-router-dom'

export interface IModelSelectorProps {
    projectId: string
    value?: string
    onChange?: (newValue: string) => void
    overrides?: SelectProps['overrides']
    disabled?: boolean
}

export default function ModelSelector({ projectId, value, onChange, overrides, disabled }: IModelSelectorProps) {
    const [keyword, setKeyword] = useState<string>()
    const [options, setOptions] = useState<{ id: string; label: React.ReactNode }[]>([])
    const modelsInfo = useQuery(`listModels:${keyword}`, () =>
        listModels(projectId, { start: 0, count: 100, search: keyword })
    )

    const handleModelInputChange = _.debounce((term: string) => {
        if (!term) {
            setOptions([])
            return
        }
        setKeyword(term)
    })

    useEffect(() => {
        if (modelsInfo.isSuccess) {
            setOptions(
                modelsInfo.data?.list.map((item) => ({
                    id: item.id,
                    label: item.name,
                })) ?? []
            )
        } else {
            setOptions([])
        }
    }, [modelsInfo.data?.list, modelsInfo.isSuccess])

    return (
        <Select
            disabled={disabled}
            overrides={overrides}
            isLoading={modelsInfo.isFetching}
            options={options}
            onChange={(params) => {
                if (!params.option) {
                    return
                }
                console.log('-', params)
                onChange?.(params.option.id as string)
            }}
            onInputChange={(e) => {
                const target = e.target as HTMLInputElement
                handleModelInputChange(target.value)
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
