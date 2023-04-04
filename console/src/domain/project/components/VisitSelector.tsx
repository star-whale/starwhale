import React from 'react'
import Select from '@starwhale/ui/Select'
import useTranslation from '@/hooks/useTranslation'
import { expandBorder } from '@starwhale/ui/utils'
import IconFont from '@starwhale/ui/IconFont'

export interface IRoleSelectorProps {
    value?: string
    onChange?: (newValue: string) => void
}
export enum VisitBy {
    Visited = 'visited',
    Latest = 'latest',
    Oldest = 'oldest',
}

export default function VisitSelector({ value = VisitBy.Visited, onChange }: IRoleSelectorProps) {
    const [t] = useTranslation()

    const options = [
        { id: VisitBy.Visited, label: t('project.visit.visited') },
        { id: VisitBy.Latest, label: t('project.visit.latest') },
        { id: VisitBy.Oldest, label: t('project.visit.oldest') },
    ]
    return (
        <Select
            backspaceClearsInputValue={false}
            searchable={false}
            clearable={false}
            overrides={{
                Root: {
                    style: {
                        width: 'fit-content',
                        minWidth: '100px',
                        ...expandBorder('0'),
                    },
                },
                ControlContainer: {
                    style: {
                        ...expandBorder('0'),
                    },
                },
                SelectArrow: ({ $isOpen }) => {
                    return (
                        <IconFont
                            type='arrow2'
                            kind='gray'
                            style={{
                                transform: $isOpen ? 'rotate(180deg)' : 'rotate(0deg)',
                                transition: 'transform 0.2s ease',
                            }}
                        />
                    )
                },
            }}
            options={options}
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
