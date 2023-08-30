import Select, { ISelectProps } from '@starwhale/ui/Select'
import _ from 'lodash'
import React, { useEffect, useState } from 'react'
import { useQuery } from 'react-query'
import { listModels } from '../services/model'
import useTranslation from '@/hooks/useTranslation'

export interface IModelSelectorProps {
    projectId: string
    value?: string | any[]
    onChange?: (newValue: any[]) => void
    overrides?: ISelectProps['overrides']
    disabled?: boolean
    clearable?: boolean
    multiple?: boolean
    getId?: (obj: any) => any
    placeholder?: React.ReactNode
}

export default function ModelMultiSelector({
    projectId,
    value,
    onChange,
    overrides,
    disabled,
    multiple = true,
    clearable = false,
    placeholder,
    getId = (obj) => obj.id,
}: IModelSelectorProps) {
    const [t] = useTranslation()
    const [keyword, setKeyword] = useState<string>()
    const [options, setOptions] = useState<{ id: string; label: React.ReactNode }[]>([])
    const modelsInfo = useQuery(
        `listModels:${projectId}:${keyword}`,
        () => listModels(projectId, { pageNum: 1, pageSize: 100, search: keyword }),
        {
            enabled: !!projectId,
        }
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
                    id: getId(item),
                    label: item.name,
                })) ?? []
            )
        } else {
            setOptions([])
        }
    }, [modelsInfo.data?.list, modelsInfo.isSuccess, getId])

    const $value = React.useMemo(() => {
        if (!value) return []
        if (typeof value === 'string')
            return String(value)
                .split(',')
                .map((item: any) => ({ id: item }))

        return (
            value?.map((item) => ({
                id: item,
            })) ?? []
        )
    }, [value])

    return (
        <Select
            multi={multiple}
            placeholder={placeholder ?? t('model.selector.placeholder')}
            disabled={disabled}
            overrides={overrides}
            clearable={clearable}
            isLoading={modelsInfo.isFetching}
            options={options}
            onChange={(params) => {
                if (params.type === 'clear') {
                    onChange?.([])
                    return
                }
                if (!params.option) {
                    return
                }
                const next = params.value.map((item) => (item.id as string) ?? '').filter((name) => name !== '')
                onChange?.(next)
            }}
            onInputChange={(e) => {
                const target = e.target as HTMLInputElement
                handleModelInputChange(target.value)
            }}
            value={$value}
        />
    )
}
