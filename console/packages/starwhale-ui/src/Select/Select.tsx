import { Select as BaseSelect, SelectProps, SIZE } from '../base/select'
import React from 'react'
import IconFont from '../IconFont'
import { mergeOverrides } from '../utils'
import useTranslation from '@/hooks/useTranslation'

export { SIZE } from 'baseui/select'

export interface ISelectProps extends SelectProps {
    overrides?: SelectProps['overrides']
    size?: keyof typeof SIZE
}

export function Select({ size = 'compact', ...props }: ISelectProps) {
    const [t] = useTranslation()
    const overrides = mergeOverrides(
        {
            ControlContainer: {
                style: {
                    'borderTopWidth': '1px',
                    'borderBottomWidth': '1px',
                    'borderLeftWidth': '1px',
                    'borderRightWidth': '1px',
                    ':hover': {
                        borderColor: '#799EE8',
                    },
                },
            },
            DropdownListItem: {
                style: {
                    ':hover': {
                        backgroundColor: '#EBF1FF',
                    },
                },
            },
            SelectArrow: ({ $isOpen }) => {
                return (
                    <IconFont
                        type='arrow_down'
                        kind='gray'
                        style={{
                            transform: $isOpen ? 'rotate(180deg)' : 'rotate(0deg)',
                            transition: 'transform 0.2s ease',
                        }}
                    />
                )
            },
            IconsContainer: {
                style: {
                    color: 'rgba(2, 16, 43, 0.2)',
                },
            },
            Tag: {
                props: {
                    overrides: {
                        Root: {
                            style: {
                                cursor: 'pointer',
                                color: 'rgba(2, 16, 43, 0.2)',
                                backgroundColor: 'rgba(2, 16, 43, 0)',
                                marginTop: '2px',
                                marginBottom: '2px',
                                marginRight: '2px',
                                marginLeft: '2px',
                            },
                        },
                    },
                },
            },
        },
        props.overrides
    )

    // eslint-disable-next-line no-param-reassign
    props.placeholder ??= t('selector.placeholder')

    // eslint-disable-next-line  react/jsx-props-no-spreading
    return <BaseSelect size={size} {...props} overrides={overrides} />
}

export function FormSelect({
    size = 'compact',
    value,
    onChange,
    ...props
}: Omit<ISelectProps, 'value' | 'onChange'> & {
    value: string | number
    onChange?: (value: string | number) => void
}) {
    return (
        <Select
            size={size}
            {...props}
            // eslint-disable-next-line
            value={
                value
                    ? [
                          {
                              id: value,
                          },
                      ]
                    : []
            }
            onChange={(params) => {
                if (!params.option) {
                    return
                }
                onChange?.(params.option.id as any)
            }}
        />
    )
}
