import { DatePicker as BaseDatePicker } from 'baseui/datepicker'
import type { DatepickerProps } from 'baseui/datepicker'
import React from 'react'
import { mergeOverrides } from '../utils'

export interface IDatePickerProps extends DatepickerProps {
    overrides?: DatepickerProps['overrides']
    // value?: string
    // onChange?: (value?: string) => void
    // disabled?: boolean
}

export function DatePicker({ size = 'compact', ...rest }: IDatePickerProps) {
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

    // eslint-disable-next-line  react/jsx-props-no-spreading
    return (
        <BaseDatePicker
            size={size}
            {...rest}
            density='high'
            overrides={overrides}
            // clearable
            // disabled={disabled}
            // value={value ? moment(value).toDate() : undefined}
            // onChange={(e) => {
            //     const date = Array.isArray(e.date) ? e.date[0] : e.date
            //     onChange?.(date ? moment(date).startOf('day').toDate().toISOString() : undefined)
            // }}
        />
    )
}

export default DatePicker
