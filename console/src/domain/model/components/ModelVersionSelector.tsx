import { formatTimestampDateTime } from '@/utils/datetime'
import { Select, SelectProps } from 'baseui/select'
import _ from 'lodash'
import React, { useEffect, useImperativeHandle, useState } from 'react'
import { useQuery, UseQueryResult } from 'react-query'
import { IListSchema } from '@/domain/base/schemas/list'
import { listModelVersions } from '../services/modelVersion'
import { IModelVersionSchema } from '../schemas/modelVersion'
/* eslint-disable react/require-default-props */

export interface IModelVersionSelectorProps {
    projectId: string
    modelId?: string
    value?: string
    onChange?: (newValue: string, item: IModelVersionSchema) => void
    overrides?: SelectProps['overrides']
    disabled?: boolean
    autoSelected?: boolean
}

export interface IDataSelectorRef<T> {
    getData: () => UseQueryResult<IListSchema<T>>
}

const ModelVersionSelector = React.forwardRef<IDataSelectorRef<any>, IModelVersionSelectorProps>(
    (
        {
            projectId,
            modelId = '',
            value = '',
            onChange = () => {},
            overrides = undefined,
            disabled = false,
            autoSelected = false,
        },
        ref
    ) => {
        const [keyword, setKeyword] = useState<string>()
        const [options, setOptions] = useState<{ id: string; label: React.ReactNode }[]>([])
        const api = useQuery(
            `listModelVersions:${projectId}:${modelId}:${keyword}`,
            () => listModelVersions(projectId, modelId as string, { pageNum: 1, pageSize: 100, search: keyword }),
            { enabled: !!modelId }
        )
        const { data, isSuccess, isFetching } = api

        useImperativeHandle(
            ref,
            () => ({
                getData: () => api,
            }),
            [api]
        )

        // eslint-disable-next-line react-hooks/exhaustive-deps
        const handleSearchInputChange = React.useCallback(
            _.debounce((term: string) => {
                if (!term) {
                    setOptions([])
                    return
                }
                setKeyword(term)
            }),
            [setKeyword, setOptions]
        )

        const handelChange = React.useCallback(
            (id?: string) => {
                const item = data?.list.find((v) => v.id === id)
                if (!item || !id) return
                onChange?.(id, item)
            },
            [data, onChange]
        )

        useEffect(() => {
            if (autoSelected) {
                if (value) {
                    const item = data?.list.find((v) => v.id === value)
                    if (!item) {
                        handelChange?.(data?.list[0]?.id)
                    }
                    return
                }
                if (data) handelChange?.(data?.list[0]?.id)
            }
        }, [value, autoSelected, modelId, data, handelChange])

        useEffect(() => {
            if (isSuccess) {
                const ops =
                    data?.list.map((item) => ({
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
        }, [data?.list, isSuccess])

        return (
            <Select
                size='compact'
                disabled={disabled}
                overrides={overrides}
                isLoading={isFetching}
                options={options}
                onChange={(params) => handelChange?.(params.option?.id as string)}
                onInputChange={(e) => handleSearchInputChange((e.target as HTMLInputElement).value)}
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
)

export default ModelVersionSelector
