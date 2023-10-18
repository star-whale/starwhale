import React from 'react'
import PopoverContainer, { Label } from './PopoverContainer'
import { StatefulCalendar } from 'baseui/datepicker'
import { useTrace } from '@starwhale/core'
import FieldInput from './FieldInput'
import moment from 'moment'
import { useControllableValue, useCreation, useKeyPress } from 'ahooks'
import { DATETIME_DELIMITER } from '@starwhale/core/datastore/schemas/TableQueryFilter'

export const DEFAULT_DATE_FORMAT = 'YYYY-MM-DD hh:mm:ss'

const INPUT_DELIMITER = DATETIME_DELIMITER

export function formatTimestampDateTime(s: Date | null | undefined, format = 'YYYY-MM-DD HH:mm:ss'): string {
    return moment.tz(s, moment.tz.guess()).format(format)
}

function getNullDatePlaceholder(formatString: string) {
    return formatString.split(INPUT_DELIMITER)[0].replace(/[0-9]|[a-z]/g, ' ')
}

function formatDate<T extends Date>(
    date: T | undefined | null | Array<T | undefined | null>,
    formatString: string = DEFAULT_DATE_FORMAT
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

function stringToDate(value: string, formatString: string = DEFAULT_DATE_FORMAT) {
    if (!value) return undefined
    if (value.includes(INPUT_DELIMITER)) {
        const [start, end] = value.split(INPUT_DELIMITER)
        return [start, end].map((v) => moment(v, formatString).toDate())
    }
    return moment(value, formatString).toDate()
}

function FieldDatetime({ options: renderOptions = [], optionFilter = () => true, isEditing = false, multi, ...rest }) {
    const trace = useTrace('field-datatime')
    const [value, setValue] = React.useState(() => stringToDate(rest.value))
    const [input, setInput] = useControllableValue<string>(rest.sharedInputProps)

    const Content = useCreation(
        () => () => {
            // eslint-disable-next-line
            const initialValue = multi ? (Array.isArray(value) ? value : [value, undefined]) : value
            return (
                <StatefulCalendar
                    initialState={{
                        value: initialValue,
                    }}
                    range={multi}
                    // value={multi ? (Array.isArray(value) ? value : [value, undefined]) : value}
                    quickSelect={multi}
                    timeSelectStart
                    timeSelectEnd
                    onChange={({ date }: any) => {
                        trace('calendar on change', date)
                        setValue(date)
                        setInput(formatDate(date))
                    }}
                />
            )
        },
        [rest.value, multi]
    )

    const ref = React.useRef<any>(null)

    // trace('render', { rest, Content }, { isEditing, value, input, ref: ref.current })

    useKeyPress(
        'enter',
        (e) => {
            if (!isEditing) return
            e.stopPropagation()
            trace('enter')
            rest.onChange?.(input)
        },
        {
            target: ref,
        }
    )

    if (!isEditing) return <Label {...rest}>{rest.value}</Label>

    return (
        <div ref={ref}>
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
                    <FieldInput width={400} focused inputRef={rest.inputRef} value={input} onChange={setInput} />
                )}
                {!isEditing && (
                    <Label {...rest}>
                        {Array.isArray(rest.value) ? rest.value.join(',') : rest.value} {rest.renderAfter?.()}
                    </Label>
                )}
            </PopoverContainer>
        </div>
    )
}

export default FieldDatetime
