import { formatTimestampDateTime } from '@/utils/datetime'
import { Select, SelectProps } from 'baseui/select'
import _ from 'lodash'
import React, { useEffect, useState } from 'react'
import { useQuery } from 'react-query'
import { listRuntimeVersions } from '../services/runtimeVersion'

export interface IRuntimeVersionSelectorProps {
    projectId: string
    runtimeId?: string
    value?: string
    onChange?: (newValue: string) => void
    overrides?: SelectProps['overrides']
    disabled?: boolean
    autoSelected?: boolean
}

export default function RuntimeVersionSelector({
    projectId,
    runtimeId,
    value,
    onChange,
    overrides,
    disabled,
    autoSelected,
}: IRuntimeVersionSelectorProps) {
    const [keyword, setKeyword] = useState<string>()
    const [options, setOptions] = useState<{ id: string; label: React.ReactNode }[]>([])
    const runtimeVersionsInfo = useQuery(
        `listRuntimeVersions:${projectId}:${runtimeId}:${keyword}`,
        () => listRuntimeVersions(projectId, runtimeId as string, { pageNum: 1, pageSize: 100, search: keyword }),
        { enabled: !!runtimeId }
    )

    const handleRuntimeVersionInputChange = _.debounce((term: string) => {
        if (!term) {
            setOptions([])
            return
        }
        setKeyword(term)
    })

    useEffect(() => {
        if (autoSelected) {
            if (value) {
                const item = runtimeVersionsInfo.data?.list.find((v) => v.id === value)
                if (!item) {
                    onChange?.(runtimeVersionsInfo.data?.list[0]?.id ?? '')
                }
                return
            }

            if (runtimeVersionsInfo.data) onChange?.(runtimeVersionsInfo.data?.list[0]?.id ?? '')
        }
    }, [value, autoSelected, runtimeId, runtimeVersionsInfo.data, onChange])

    useEffect(() => {
        if (runtimeVersionsInfo.isSuccess) {
            const ops =
                runtimeVersionsInfo.data?.list.map((item) => ({
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
    }, [runtimeVersionsInfo.data?.list, runtimeVersionsInfo.isSuccess])

    return (
        <Select
            size='compact'
            disabled={disabled}
            overrides={overrides}
            isLoading={runtimeVersionsInfo.isFetching}
            options={options}
            clearable={false}
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
