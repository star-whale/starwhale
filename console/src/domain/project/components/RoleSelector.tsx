import { Select, SIZE } from 'baseui/select'
import React from 'react'
import { useFetchRoleList } from '@project/hooks/useFetchRoleList'

export interface IRoleSelectorProps {
    value?: string
    onChange?: (newValue: string) => void
}

export default function RoleSelector({ value, onChange }: IRoleSelectorProps) {
    const roles = useFetchRoleList()

    return (
        <Select
            size={SIZE.compact}
            clearable={false}
            isLoading={roles.isFetching}
            options={(roles.data ?? []).map((role) => {
                return { id: role.id, label: role.name }
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
