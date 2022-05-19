import { Select, SelectProps } from 'baseui/select'
import React from 'react'

export interface IMultiTagsProps {
    value?: string[]
    placeholder?: string
    onChange?: (newValue: string[]) => void
    getValueLabel?: SelectProps['getValueLabel']
}

export default function MultiTags({ value, placeholder, onChange, getValueLabel }: IMultiTagsProps) {
    return (
        <Select
            clearable={false}
            closeOnSelect={false}
            options={[]}
            openOnClick={false}
            overrides={{
                SelectArrow: {
                    props: {
                        overrides: {
                            Svg: {
                                style: {
                                    display: 'none',
                                },
                            },
                        },
                    },
                },
                Dropdown: {
                    style: {
                        display: 'none',
                    },
                },
                Tag: {
                    props: {
                        overrides: {
                            Root: {
                                style: {
                                    backgroundColor: 'var(--color-brandPrimary)',
                                },
                            },
                        },
                    },
                },
            }}
            getValueLabel={getValueLabel}
            value={value?.map((item) => ({
                id: item,
                label: item,
            }))}
            multi
            placeholder={placeholder}
            onChange={(params) => {
                onChange?.(params.value.map((item) => (item.id as string) ?? '').filter((name) => name !== ''))
            }}
        />
    )
}
