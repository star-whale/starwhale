import { useStyletron } from 'baseui'
import { Select, SelectProps } from 'baseui/select'
import React from 'react'

export interface IMultiTagsProps {
    value?: string[]
    placeholder?: string
    onChange?: (newValue: string[]) => void
    getValueLabel?: SelectProps['getValueLabel']
}

export default function MultiTags({ value, placeholder, onChange, getValueLabel }: IMultiTagsProps) {
    const [, theme] = useStyletron()

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
                                    'cursor': 'pointer',
                                    'backgroundColor': 'var(--color-brandPrimary)',
                                    'marginTop': '2px',
                                    'marginBottom': '2px',
                                    'marginRight': '2px',
                                    'marginLeft': '2px',

                                    ':hover': {
                                        backgroundColor: 'var(--color-brandPrimaryHover)',
                                    },
                                },
                            },
                            Text: {
                                style: {
                                    font: {
                                        ...theme.typography.LabelXSmall,
                                        lineHeight: '20px',
                                    },
                                },
                            },
                        },
                    },
                },
            }}
            size='compact'
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
