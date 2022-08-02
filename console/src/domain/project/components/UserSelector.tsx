import { Select, SIZE } from 'baseui/select'
import React from 'react'
import { useFetchUsers } from '@user/hooks/useUser'
import { usePage } from '@/hooks/usePage'

export interface IUserSelectorProps {
    value?: string
    onChange?: (newValue: string) => void
    ignoreIds?: string[]
}

export default function UserSelector({ value, onChange, ignoreIds }: IUserSelectorProps) {
    const ignores = ignoreIds ?? []
    // TODO make user searchable by backend
    const [page] = usePage()
    const users = useFetchUsers(page)

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
