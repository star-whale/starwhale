import Select from '@starwhale/ui/Select'
import React from 'react'
import { useFetchProjects } from '../hooks/useFetchProjects'

export interface IProjectSelectorProps {
    value?: string
    onChange?: (newValue: string) => void
    ignoreIds?: string[]
    autoSelected?: boolean
}

export default function ProjectSelector({ value, onChange, ignoreIds, autoSelected = true }: IProjectSelectorProps) {
    const ignores = ignoreIds ?? []
    const info = useFetchProjects({ pageNum: 1, pageSize: 99999 /* do not support pagination for now */ })
    const first = info.data?.list?.[0] as any

    React.useEffect(() => {
        if (autoSelected && first) {
            onChange?.(first.id)
        }
        // eslint-disable-next-line react-hooks/exhaustive-deps
    }, [autoSelected, first])

    return (
        <Select
            clearable={false}
            required
            isLoading={info.isFetching}
            options={(info.data?.list ?? [])
                .map((tmp) => {
                    return { id: tmp.id, label: tmp.name }
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
