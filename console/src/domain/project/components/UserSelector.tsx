import { Select, SIZE } from 'baseui/select'
import React from 'react'
import { useFetchUsers } from '@user/hooks/useUser'

export interface IUserSelectorProps {
    value?: string
    onChange?: (newValue: string) => void
    ignoreIds?: string[]
}

export default function UserSelector({ value, onChange, ignoreIds }: IUserSelectorProps) {
    const ignores = ignoreIds ?? []
    // TODO make user searchable by backend
    const users = useFetchUsers({ pageNum: 1, pageSize: 99999 /* do not support pagination for now */ })

    return (
        <Select
            size={SIZE.compact}
            clearable={false}
            required
            isLoading={users.isFetching}
            options={(users.data?.list ?? [])
                .map((user) => {
                    return { id: user.id, label: user.name }
                })
                .filter((opt) => !ignores.includes(opt.id))}
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
