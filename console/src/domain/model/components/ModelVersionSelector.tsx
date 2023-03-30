import { formatTimestampDateTime } from '@/utils/datetime'
import Select, { ISelectProps } from '@starwhale/ui/Select'
import _ from 'lodash'
import React, { useEffect, useImperativeHandle, useState } from 'react'
import { useQuery, UseQueryResult } from 'react-query'
import { IListSchema } from '@/domain/base/schemas/list'
import { listModelVersions } from '../services/modelVersion'
import { IModelVersionSchema } from '../schemas/modelVersion'
import { useEventCallback } from '@starwhale/core/utils'
import { ModelLabel } from './ModelLabel'
/* eslint-disable react/require-default-props */

export interface IModelVersionSelectorProps {
    projectId: string
    modelId?: string
    value?: string
    onChange?: (newValue: string, item: IModelVersionSchema) => void
    overrides?: ISelectProps['overrides']
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
            { enabled: !!modelId, refetchOnWindowFocus: false }
        )
        const { data, isSuccess, isError, isFetching } = api

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
                setKeyword(term)
            }),
            [setKeyword, setOptions]
        )

        const handelChange = useEventCallback((id?: string) => {
            const item = data?.list.find((v) => v.id === id)
            if (!item || !id) return
            onChange?.(id, item)
        })

        useEffect(() => {
            if (!data?.list || data?.list.length === 0 || !autoSelected) return

            if (value) {
                const item = data?.list.find((v) => v.id === value)
                if (!item) {
                    handelChange?.(data?.list[0]?.id)
                }
                return
            }
            if (data) handelChange?.(data?.list[0]?.id)
        }, [value, autoSelected, modelId, data, handelChange])

        const $options = React.useMemo(() => {
            if (!isSuccess) return []
            const ops =
                data?.list.map((item) => ({
                    id: item.id,
                    label: <ModelLabel version={item} />,
                })) ?? []
            return ops
        }, [data, isSuccess, isError, isFetching])

        return (
            <Select
                disabled={disabled}
                overrides={overrides}
                isLoading={isFetching}
                options={$options}
                clearable={false}
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
