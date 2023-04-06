import { Select as BaseSelect, SelectProps, SIZE } from 'baseui/select'
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
        },
        props.overrides
    )

    // eslint-disable-next-line no-param-reassign
    props.placeholder ??= t('selector.placeholder')

    // eslint-disable-next-line  react/jsx-props-no-spreading
    return <BaseSelect size={size} {...props} overrides={overrides} />
}
