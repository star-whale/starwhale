import { formatTimestampDateTime } from '@/utils/datetime'
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
    autoSelected?: boolean
}

export default function ModelVersionSelector({
    projectId,
    modelId,
    value,
    onChange,
    overrides,
    disabled,
    autoSelected,
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
        if (autoSelected) {
            if (value) {
                const item = modelVersionsInfo.data?.list.find((v) => v.id === value)
                if (!item) {
                    onChange?.(modelVersionsInfo.data?.list[0]?.id ?? '')
                }
                return
            }
            if (modelVersionsInfo.data) onChange?.(modelVersionsInfo.data?.list[0]?.id ?? '')
        }
    }, [value, autoSelected, modelId, modelVersionsInfo.data, onChange])

    useEffect(() => {
        if (modelVersionsInfo.isSuccess) {
            const ops =
                modelVersionsInfo.data?.list.map((item) => ({
                    id: item.id,
                    label: [
                        item.alias ?? '',
                        item.name ? item.name.substring(0, 8) : '',
                        item.createdTime ? formatTimestampDateTime(item.createdTime) : '',
                    ].join(' : '),
                })) ?? []
            setOptions(ops)
        } else {
            setOptions([])
        }
    }, [modelVersionsInfo.data?.list, modelVersionsInfo.isSuccess])

    return (
        <Select
            size='compact'
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
