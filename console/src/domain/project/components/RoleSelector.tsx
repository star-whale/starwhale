import Select from '@starwhale/ui/Select'
import React from 'react'
import { useFetchRoleList } from '@project/hooks/useFetchRoleList'
import useTranslation from '@/hooks/useTranslation'

export interface IRoleSelectorProps {
    value?: string
    onChange?: (newValue: string) => void
}

export default function RoleSelector({ value, onChange }: IRoleSelectorProps) {
    const [t] = useTranslation()
    const roles = useFetchRoleList()

    return (
        <Select
            clearable={false}
            isLoading={roles.isFetching}
            options={(roles.data ?? []).map((role) => {
                return { id: role.id, label: t(role.name as any) }
            })}
            overrides={
                {
                    // Root: {
                    //     style: {
                    //         width: 'fit-content',
                    //         minWidth: '100px',
                    //         ...expandBorder('0'),
                    //     },
                    // },
                    // ControlContainer: {
                    //     style: {
                    //         ...expandBorder('0'),
                    //     },
                    // },
                    // SelectArrow: ({ $isOpen }) => {
                    //     return (
                    //         <IconFont
                    //             type='arrow2'
                    //             kind='gray'
                    //             style={{
                    //                 transform: $isOpen ? 'rotate(180deg)' : 'rotate(0deg)',
                    //                 transition: 'transform 0.2s ease',
                    //             }}
                    //         />
                    //     )
                    // },
                }
            }
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
