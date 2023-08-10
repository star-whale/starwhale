import Select from '@starwhale/ui/Select'
import React from 'react'
import { useFetchProjects } from '../hooks/useFetchProjects'

export interface IProjectSelectorProps {
    value?: string
    onChange?: (newValue: string) => void
    onChangeItem?: (item: any, list?: any[]) => void
    ignoreIds?: string[]
    autoSelected?: boolean
}

export default function ProjectSelector({
    value,
    onChange,
    onChangeItem,
    ignoreIds,
    autoSelected = true,
}: IProjectSelectorProps) {
    const ignores = ignoreIds ?? []
    const info = useFetchProjects({ pageNum: 1, pageSize: 99999 /* do not support pagination for now */ })
    const first = info.data?.list?.[0] as any

    React.useEffect(() => {
        if (autoSelected && first) {
            onChange?.(first.id)
            onChangeItem?.(first)
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
                const project = info.data?.list?.find((tmp) => tmp.id === option.id)
                onChange?.(option.id as string)
                onChangeItem?.(project, info.data?.list)
            }}
            value={[{ id: value }]}
        />
    )
}
