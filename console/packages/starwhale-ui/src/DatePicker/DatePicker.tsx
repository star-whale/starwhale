import { DatePicker as BaseDatePicker } from 'baseui/datepicker'
import type { DatepickerProps } from 'baseui/datepicker'
import React from 'react'
import { mergeOverrides } from '../utils'
import moment from 'moment'
import zh from 'date-fns/locale/zh-CN'
import useTranslation from '@/hooks/useTranslation'

export interface IDatePickerProps extends DatepickerProps {
    overrides?: DatepickerProps['overrides']
}

export function DatePicker({ size = 'compact', value, ...rest }: IDatePickerProps) {
    const [, i18n] = useTranslation()
    const overrides = mergeOverrides(
        {
            QuickSelect: {
                style: {
                    'borderTopWidth': '1px',
                    'borderBottomWidth': '1px',
                    'borderLeftWidth': '1px',
                    'borderRightWidth': '1px',
                    'paddingLeft': '0px',
                    'paddingRight': '0px',
                    ':hover': {
                        borderColor: '#799EE8',
                    },
                    ':focus': {
                        backgroundColor: '#fff',
                    },
                },
            },
        },
        rest.overrides
    )

    const $value = React.useMemo(() => {
        if (!value) return undefined
        if (Array.isArray(value)) {
            return value.map((v) => (v ? moment(v).toDate() : null))
        }
        return moment(value).toDate()
    }, [value])

    // @ts-ignore
    return (
        <BaseDatePicker
            locale={i18n.language === 'zh' ? zh : undefined}
            size={size}
            density='high'
            overrides={overrides}
            value={$value}
            {...rest}
        />
    )
}

export default DatePicker
