import React from 'react'
import PopoverContainer, { Label } from './PopoverContainer'
import { StatefulCalendar } from 'baseui/datepicker'
import { useTrace } from '@starwhale/core'
import FieldInput from './FieldInput'
import moment from 'moment'
import { useKeyPress } from 'ahooks'

export const DEFAULT_DATE_FORMAT = 'YYYY-MM-DD hh:mm:ss'

const INPUT_DELIMITER = '~'

export function formatTimestampDateTime(s: Date | null | undefined, format = 'YYYY-MM-DD HH:mm:ss'): string {
    return moment.tz(s, moment.tz.guess()).format(format)
}

function getNullDatePlaceholder(formatString: string) {
    return formatString.split(INPUT_DELIMITER)[0].replace(/[0-9]|[a-z]/g, ' ')
}

function formatDate<T>(
    date: T | undefined | null | Array<T | undefined | null>,
    formatString: string = DEFAULT_DATE_FORMAT,
    locale = undefined
) {
    const format = formatTimestampDateTime

    if (!date) {
        return ''
    }
    if (Array.isArray(date) && !date[0] && !date[1]) {
        return ''
    }
    if (Array.isArray(date) && !date[0] && date[1]) {
        const endDate = format(date[1])
        const startDate = getNullDatePlaceholder(formatString)
        return [startDate, endDate].join(`${INPUT_DELIMITER}`)
    }
    if (Array.isArray(date)) {
        return date.map((day) => (day ? format(day) : '')).join(`${INPUT_DELIMITER}`)
    }
    return format(date)
}

function FieldDatetime({ options: renderOptions = [], optionFilter = () => true, isEditing = false, ...rest }) {
    const trace = useTrace('field-datatime')
    const [value, setValue] = React.useState(formatDate(Date.now()))
    // const [input, setInput] = React.useState('')
    const { inputRef, onChange: onInputChange, value: input } = rest.sharedInputProps || {}

    const Content = React.useCallback((props) => {
        trace('content', props)
        return (
            <StatefulCalendar
                range
                value={value}
                quickSelect
                timeSelectStart
                timeSelectEnd
                onChange={({ date, ...args }) => {
                    console.log('onchange', date, args)
                    const v = formatDate(date)
                    trace('onchange', v)
                    setValue(date)
                    onInputChange(formatDate(date))
                }}
            />
        )
    }, [])

    const ref = React.useRef<HTMLInputElement>(null)

    useKeyPress('enter', (e) => {
        e.stopPropagation()
        trace('enter')
        rest.onChange?.(input)
    })

    useKeyPress('backspace', (e) => {
        e.stopPropagation()
        trace('backspace')
    })

    if (!isEditing) return <Label {...rest}>{rest.value}</Label>

    trace(value, { ref: ref.current })

    return (
        <PopoverContainer
            {...rest}
            options={renderOptions.filter(optionFilter)}
            // only option exsit will show popover
            isOpen={isEditing}
            onItemSelect={({ item }) => rest.onChange?.(item.type)}
            onItemIdsChange={(ids = []) => rest.onChange?.(ids.join(','))}
            Content={Content}
        >
            {isEditing && (
                <FieldInput
                    width={400}
                    focused
                    inputRef={inputRef}
                    value={input}
                    onChange={onInputChange}
                    // onChange={(e) => setValue(e.target.value.split(','))}
                    // {...rest.sharedInputProps}
                />
            )}
            {!isEditing && (
                <Label {...rest}>
                    {Array.isArray(rest.value) ? rest.value.join(',') : rest.value} {rest.renderAfter?.()}
                </Label>
            )}
        </PopoverContainer>
    )
}

export default FieldDatetime
