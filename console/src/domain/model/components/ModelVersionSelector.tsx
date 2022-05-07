import { Select, SelectProps } from 'baseui/select'
import _ from 'lodash'
import React, { useEffect, useState } from 'react'
import { useQuery } from 'react-query'
import { listModelVersions } from '../services/modelVersion'

export interface IModelVersionSelectorProps {
    projectId: string
    modelId?: string
    value?: string
    onChange?: (newValue: string) => void
    overrides?: SelectProps['overrides']
    disabled?: boolean
}

export default function ModelVersionSelector({
    projectId,
    modelId,
    value,
    onChange,
    overrides,
    disabled,
}: IModelVersionSelectorProps) {
    const [keyword, setKeyword] = useState<string>()
    const [options, setOptions] = useState<{ id: string; label: React.ReactNode }[]>([])
    const modelVersionsInfo = useQuery(
        `listModelVersions:${projectId}:${modelId}:${keyword}`,
        () => listModelVersions(projectId, modelId as string, { pageNum: 1, pageSize: 100, search: keyword }),
        { enabled: !!modelId }
    )

    const handleModelVersionInputChange = _.debounce((term: string) => {
        if (!term) {
            setOptions([])
            return
        }
        setKeyword(term)
    })

    useEffect(() => {
        if (modelVersionsInfo.isSuccess) {
            setOptions(
                modelVersionsInfo.data?.list.map((item) => ({
                    id: item.id,
                    label: item.name,
                })) ?? []
            )
        } else {
            setOptions([])
        }
    }, [modelVersionsInfo.data?.list, modelVersionsInfo.isSuccess])

    return (
        <Select
            disabled={disabled}
            overrides={overrides}
            isLoading={modelVersionsInfo.isFetching}
            options={options}
            onChange={(params) => {
                if (!params.option) {
                    return
                }
                onChange?.(params.option.id as string)
            }}
            onInputChange={(e) => {
                const target = e.target as HTMLInputElement
                handleModelVersionInputChange(target.value)
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
