import * as React from 'react'
import { StatefulCalendar as BaseStatefulCalendar, DatepickerProps } from 'baseui/datepicker'
import useTranslation from '@/hooks/useTranslation'
import { mergeOverrides, expandPadding } from '../utils'
import zh from 'date-fns/locale/zh-CN'

const selectOverride = {
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
    props: {
        overrides: {
            ValueContainer: {
                style: {
                    ...expandPadding('4px', '12px', '4px', '12px'),
                    fontSize: '14px',
                },
            },
            IconsContainer: {
                style: {
                    color: 'rgba(2, 16, 43, 0.2)',
                },
            },
        },
    },
}

function StatefulCalendar({ ...props }: DatepickerProps) {
    const [, i18n] = useTranslation()
    const overrides = mergeOverrides(
        {
            QuickSelect: selectOverride,
            TimeSelectContainer: {
                style: {
                    width: props.range ? '50%' : '100%',
                    display: 'inline-block',
                    marginBottom: '0',
                },
            },
            TimeSelect: {
                props: {
                    overrides: {
                        Select: selectOverride,
                    },
                },
            },
        },
        props.overrides
    )

    return (
        <BaseStatefulCalendar
            density='high'
            locale={i18n.language === 'zh' ? { ...zh } : undefined}
            {...props}
            overrides={overrides}
        />
    )
}

export { StatefulCalendar }
