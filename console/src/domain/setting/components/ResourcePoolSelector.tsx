import { Select, SIZE } from 'baseui/select'
import React, { useEffect } from 'react'
import { useFetchSystemResourcePool } from '@/domain/setting/hooks/useSettings'
import { ISystemResourcePool } from '../schemas/system'

export interface IUserSelectorProps {
    value?: string
    onChange?: (newValue: string) => void
    onChangeItem?: (item: ISystemResourcePool, list?: ISystemResourcePool[]) => void
    autoSelected?: boolean
}

export default function ResourcePoolSelector({
    value,
    onChange,
    onChangeItem,
    autoSelected = false,
}: IUserSelectorProps) {
    const pools = useFetchSystemResourcePool()

    const handelChange = React.useCallback(
        (id: string) => {
            const { data } = pools
            const item = data?.find((v) => v.name === id) ?? (data?.[0] as any)
            if (value !== id) {
                onChange?.(id)
                onChangeItem?.(item, data)
            }
        },
        [onChange, onChangeItem, pools, value]
    )

    useEffect(() => {
        if (autoSelected) {
            const { data } = pools
            const first = data?.[0] as any
            if (value) handelChange(value)
            else if (data) handelChange(first.name)
        }
    }, [pools, value, autoSelected, handelChange])

    return (
        <Select
            size={SIZE.compact}
            clearable={false}
            required
            isLoading={pools.isFetching}
            options={(pools.data ?? []).map(({ name }) => {
                return { id: name, label: name }
            })}
            onChange={({ option }) => {
                if (!option) {
                    return
                }
                handelChange(option.id as string)
            }}
            value={[{ id: value }]}
        />
    )
}
