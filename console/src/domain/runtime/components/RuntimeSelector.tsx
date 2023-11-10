import Select, { ISelectProps } from '@starwhale/ui/Select'
import _ from 'lodash'
import React, { useEffect, useState } from 'react'
import { useQuery } from 'react-query'
import { listRuntimes } from '../services/runtime'

export interface IRuntimeSelectorProps {
    projectId: string
    value?: string
    onChange?: (newValue: string) => void
    overrides?: ISelectProps['overrides']
    disabled?: boolean
}

export default function RuntimeSelector({ projectId, value, onChange, overrides, disabled }: IRuntimeSelectorProps) {
    const [keyword, setKeyword] = useState<string>()
    const [options, setOptions] = useState<{ id: string; label: React.ReactNode }[]>([])
    const runtimesInfo = useQuery(`listRuntimes:${projectId}:${keyword}`, () =>
        listRuntimes(projectId, { pageNum: 1, pageSize: 100, search: keyword })
    )

    const handleRuntimeInputChange = _.debounce((term: string) => {
        if (!term) {
            setOptions([])
            return
        }
        setKeyword(term)
    })

    useEffect(() => {
        if (runtimesInfo.isSuccess) {
            const ops =
                runtimesInfo.data?.list?.map((item) => ({
                    id: item.id,
                    label: item.name,
                })) ?? []
            setOptions(ops)
            // if (!value) {
            //     onChange?.(ops[0]?.id)
            // }
        } else {
            setOptions([])
        }
    }, [runtimesInfo.data?.list, runtimesInfo.isSuccess])

    return (
        <Select
            disabled={disabled}
            overrides={overrides}
            isLoading={runtimesInfo.isFetching}
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
                handleRuntimeInputChange(target.value)
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
