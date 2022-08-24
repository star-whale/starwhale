import { Select, SIZE } from 'baseui/select'
import React from 'react'
import { useFetchResourcePools } from '@job/hooks/useFetchResourcePools'

export interface IUserSelectorProps {
    value?: string
    onChange?: (newValue: string) => void
}

export default function ResourcePoolSelector({ value, onChange }: IUserSelectorProps) {
    const pools = useFetchResourcePools()

    return (
        <Select
            size={SIZE.compact}
            clearable={false}
            required
            isLoading={pools.isFetching}
            options={(pools.data ?? []).map(({ label }) => {
                return { id: label, label }
            })}
            onChange={({ option }) => {
                if (!option) {
                    return
                }
                onChange?.(option.id as string)
            }}
            value={[{ id: value }]}
        />
    )
}
