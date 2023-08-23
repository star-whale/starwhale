import Select from '@starwhale/ui/Select'
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
            // when init value specified, the id will equal to value
            // we need to trigger onChangeItem to notify the other components which depend on the resource,
            // so we do not wrap onChangeItem in the condition
            onChangeItem?.(item, data)
            if (value !== id) {
                onChange?.(id)
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
