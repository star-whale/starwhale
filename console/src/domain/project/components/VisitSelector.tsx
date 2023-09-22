import React from 'react'
import Select from '@starwhale/ui/Select'
import useTranslation from '@/hooks/useTranslation'
import { expandPadding } from '@starwhale/ui/utils'
import IconFont from '@starwhale/ui/IconFont'

export interface IVisitSelectorProps {
    value?: string
    onChange?: (newValue: string) => void
}
export enum VisitBy {
    Visited = 'visited',
    Latest = 'latest',
    Oldest = 'oldest',
}

export default function VisitSelector({ value = VisitBy.Visited, onChange }: IVisitSelectorProps) {
    const [t] = useTranslation()

    const options = [
        { id: VisitBy.Visited, label: t('project.visit.visited') },
        { id: VisitBy.Latest, label: t('project.visit.latest') },
        { id: VisitBy.Oldest, label: t('project.visit.oldest') },
    ]
    const optionsValue = {
        [VisitBy.Visited]: t('project.visit.visited'),
        [VisitBy.Latest]: t('project.visit.latest.value'),
        [VisitBy.Oldest]: t('project.visit.oldest.value'),
    }
    return (
        <Select
            backspaceClearsInputValue={false}
            searchable={false}
            clearable={false}
            overrides={{
                Root: {
                    style: {
                        width: 'fit-content',
                        minWidth: '150px',
                    },
                },
                ValueContainer: {
                    style: {
                        ...expandPadding('3px', '8px', '3px', '8px'),
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
            getValueLabel={({ option }) => {
                return option.id ? optionsValue[option.id] : ''
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
