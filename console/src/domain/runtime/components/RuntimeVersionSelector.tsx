import { Select, SelectProps } from 'baseui/select'
import _ from 'lodash'
import React, { useEffect, useState } from 'react'
import { useQuery } from 'react-query'
import { listRuntimeVersions } from '../services/runtimeVersion'

export interface IRuntimeVersionSelectorProps {
    projectId: string
    modelId?: string
    value?: string
    onChange?: (newValue: string) => void
    overrides?: SelectProps['overrides']
    disabled?: boolean
}

export default function RuntimeVersionSelector({
    projectId,
    modelId,
    value,
    onChange,
    overrides,
    disabled,
}: IRuntimeVersionSelectorProps) {
    const [keyword, setKeyword] = useState<string>()
    const [options, setOptions] = useState<{ id: string; label: React.ReactNode }[]>([])
    const runtimeVersionsInfo = useQuery(
        `listRuntimeVersions:${projectId}:${modelId}:${keyword}`,
        () => listRuntimeVersions(projectId, modelId as string, { pageNum: 1, pageSize: 100, search: keyword }),
        { enabled: !!modelId }
    )

    const handleRuntimeVersionInputChange = _.debounce((term: string) => {
        if (!term) {
            setOptions([])
            return
        }
        setKeyword(term)
    })

    useEffect(() => {
        if (runtimeVersionsInfo.isSuccess) {
            setOptions(
                runtimeVersionsInfo.data?.list.map((item) => ({
                    id: item.id,
                    label: item.name,
                })) ?? []
            )
        } else {
            setOptions([])
        }
    }, [runtimeVersionsInfo.data?.list, runtimeVersionsInfo.isSuccess])

    return (
        <Select
            size='compact'
            disabled={disabled}
            overrides={overrides}
            isLoading={runtimeVersionsInfo.isFetching}
            options={options}
            onChange={(params) => {
                if (!params.option) {
                    return
                }
                onChange?.(params.option.id as string)
            }}
            onInputChange={(e) => {
                const target = e.target as HTMLInputElement
                handleRuntimeVersionInputChange(target.value)
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
