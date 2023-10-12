import { DataTypeT, OPERATOR } from '@starwhale/core/datastore'
import React, { useEffect, useMemo, useRef, useState } from 'react'
import { useClickAway } from 'react-use'
import AutosizeInput from '../base/select/autosize-input'
import { FilterPropsT, SearchFieldSchemaT, Operators } from './types'
import IconFont from '../IconFont'
import { dataStoreToFilter } from './utils'
import { createUseStyles } from 'react-jss'
import { filterMachine } from './createFilterMachine'
import { useMachine } from '@xstate/react'
import { useTrace } from '@starwhale/core/utils'
import _ from 'lodash'

export const useStyles = createUseStyles({
    filters: {
        'position': 'relative',
        'display': 'flex',
        'flexWrap': 'nowrap',
        'gap': '1px',
        'cursor': 'pointer',
        'width': 'auto',
        'height': '22px',
        'lineHeight': '22px',
        '&:hover $label': {
            backgroundColor: '#EDF3FF',
        },
    },
    label: {
        height: '22px',
        lineHeight: '22px',
        padding: '0 8px',
        background: '#EEF1F6',
        borderRadius: '4px',
        whiteSpace: 'nowrap',
        textOverflow: 'ellipsis',
        overflow: ' hidden',
        display: 'flex',
        alignItems: 'center',
    },
})

// @ts-ignore
const containsNode = (parent, child) => {
    return child && parent && parent.contains(child as any)
}

const isValueExist = (value: any) => {
    if (value === 0) return true
    return !!value
}

export default function FilterRenderer({
    value: rawValues = {},
    onChange = () => {},
    focusToEnd = () => {},
    onRemove = () => {},
    isFocus = false,
    style = {},
    fields,
    ...rest
}: FilterPropsT & {
    fields: SearchFieldSchemaT[]
    style?: React.CSSProperties
    onClick?: () => void
    focusToEnd?: () => void
    onRemove?: (blur?: boolean) => void
    containerRef?: React.RefObject<HTMLDivElement>
}) {
    const $columns = fields
    const styles = useStyles()
    const [input, setInput] = useState<any>('')
    const ref = useRef<HTMLDivElement>(null)
    const inputRef = useRef<HTMLInputElement>(null)
    const [cached, setCached] = useState<any>(undefined)
    const trace = useTrace('grid-search-filter-render')
    const [removing, setRemoving] = useState(false)

    const origins = React.useMemo(
        () => [
            { type: 'property', value: rawValues.property },
            { type: 'op', value: rawValues.op },
            { type: 'value', value: rawValues.value },
        ],
        [rawValues]
    )

    const [machine, send] = useMachine(filterMachine, {
        context: {
            origins,
            values: origins,
        },
    })
    const { values, focusTarget, focused } = machine.context
    const [property, op, value] = values.map((tmp) => tmp.value)

    const { FilterOperator, FilterField, FilterValue, FilterFieldValue, filter } = useMemo(() => {
        const field = $columns.find((tmp) => tmp.name === property)
        const _filter = dataStoreToFilter(field?.type as DataTypeT)()
        return {
            filter: _filter,
            FilterOperator: _filter.renderOperator,
            FilterField: _filter.renderField,
            FilterFieldValue: _filter.renderFieldValue,
            FilterValue: _filter.renderValue,
        }
    }, [property, $columns])

    const isCurrentOptionMatch = React.useCallback(
        (index) => (tmp) => {
            if (index !== focusTarget) return true
            try {
                return tmp.label?.match(input)
            } catch {
                return false
            }
        },
        [focusTarget, input]
    )

    const $fieldOptions = React.useMemo(() => {
        return $columns.map((tmp) => {
            return {
                id: tmp.path,
                type: tmp.path,
                label: tmp.label,
            }
        })
    }, [$columns])

    const $operatorOptions = React.useMemo(() => {
        return filter.operators.map((key: string) => {
            const operator = Operators[key]
            return {
                id: operator.key,
                type: operator.key,
                label: operator.label,
            }
        })
    }, [filter])

    const $valueOptions = React.useMemo(() => {
        const column = $columns.find((tmp) => tmp.name === property)
        if (!column) return []
        return column.getHints()?.map((tmp) => {
            return {
                id: tmp,
                type: tmp,
                label: tmp,
            }
        })
    }, [$columns, property])

    const isAllValusExist = (_values) => _values.every((option) => isValueExist(option.value))
    const isAllValueNone = (_values) => _values.every((option) => !isValueExist(option.value))

    const cache = (_values) => {
        if (_values.every((item) => isValueExist(item.value))) {
            setCached(_.cloneDeep(_values))
        }
    }
    // esc
    const reset = () => {
        setRemoving(false)
        // @ts-ignore
        send({ type: 'RESET', cached: _.cloneDeep(cached) })
    }

    const blur = () => {
        if (!focused) return
        send({ type: 'BLUR' })
    }

    const onSubmit = (_values) => {
        cache(_values)
        const tmp = _values.reduce((acc, curr) => {
            return _.set(acc, curr.type, curr.value)
        }, {})
        blur()
        onChange(tmp)
    }

    const confirm = (v, index) => {
        const hasSearchInput = isValueExist(input)
        let next
        if (index === 2 && hasSearchInput) {
            next = send({ type: 'CONFIRM', value: input, index })
        } else {
            next = send({ type: 'CONFIRM', value: v, index })
        }
        setInput('')
        if (isAllValusExist(next.context.values)) {
            onSubmit(next.context.values)
        }
    }

    const submit = () => {
        const hasSearchInput = isValueExist(input)
        if (!property) return
        if (!op) return
        trace('submit', { hasSearchInput, value })
        // has input then reset default value
        if (hasSearchInput) {
            confirm(input, 2)
            return
        }
        // has no input then use default value
        if (value) confirm(value, 2)
    }

    const focus = () => {
        inputRef.current?.focus()
        send({ type: 'FOCUS' })
    }

    const focusOnTarget = (index = 0) => {
        focus()
        send({ type: 'FOCUSTARGET', index })
    }

    const focusOnLastEdit = () => {
        focus()
        send({ type: 'FOCUSONLASTEDIT' })
    }

    const focusRemove = () => {
        const hasSearchInput = isValueExist(input)
        // has input cancel remove
        if (hasSearchInput) {
            trace('focusRemove', 'cancel')
            return
        }
        // confirm removing
        if (removing || isAllValueNone(values)) {
            trace('focusRemove', 'removed')
            onRemove()
            setRemoving(false)
            return
        }
        trace('focusRemove', 'do remove', focusTarget)
        const next = send({ type: 'REMOVE', index: focusTarget })
        if (next.context.focusTarget === 0) {
            setRemoving(true)
        } else {
            setRemoving(false)
        }
    }

    const handleClick = () => {
        rest.onClick?.()
        focus()
    }

    const handleInputChange = (event: React.SyntheticEvent<HTMLInputElement> | any) => {
        if (typeof event === 'object' && 'target' in event) {
            setInput((event.target as any).value)
        } else {
            setInput(event ?? '')
        }
        focus()
    }

    const handleKeyDown = (event: KeyboardEvent | any) => {
        switch (event.keyCode) {
            case 27:
                reset()
                // esc trigger focus to last edit
                focusToEnd()
                break
            case 9: // tab
            case 13: // enter
                trace('enter/tab')
                submit()
                break
            case 8: // backspace
                event.stopPropagation()
                focusRemove()
                break
            default:
                break
        }
    }

    // click away
    const fieldDropdownRef = useRef(null)
    const opDropdownRef = useRef(null)
    useClickAway(ref, (e) => {
        if (containsNode(fieldDropdownRef.current, e.target)) return
        if (containsNode(opDropdownRef.current, e.target)) return
        if (containsNode(document.querySelector('.filter-popover'), e.target)) return
        reset()
    })

    // keep focus by parent component
    useEffect(() => {
        if (isFocus) {
            focusOnLastEdit()
        } else {
            blur()
        }
        // eslint-disable-next-line react-hooks/exhaustive-deps
    }, [isFocus])

    // if origins value exists then cached it
    useEffect(() => {
        cache(origins)
        // eslint-disable-next-line react-hooks/exhaustive-deps
    }, [origins])

    // trace('cached', origins, cached, rawValues)

    const Remove = (
        <div className={styles.label}>
            <div
                className='filter-remove'
                role='button'
                onClick={(e) => {
                    e.preventDefault()
                    e.stopPropagation()
                    onRemove(true)
                }}
                tabIndex={0}
            >
                <IconFont
                    type='close'
                    style={{
                        width: '12px',
                        height: '12px',
                        borderRadius: '50%',
                        backgroundColor: ' rgba(2,16,43,0.20)',
                        color: '#FFF',
                        marginLeft: '6px',
                    }}
                    size={12}
                />
            </div>
        </div>
    )

    const Input = (
        <div
            className='autosize-input inline-block relative flex-1 h-full max-w-full'
            style={{
                minWidth: focused ? '50px' : 0,
                flexBasis: focused ? '100px' : 0,
                width: focused ? '160px' : 0,
            }}
        >
            {/* @ts-ignore */}
            <AutosizeInput
                inputRef={inputRef as any}
                value={input}
                onChange={handleInputChange}
                overrides={{
                    Input: FilterValue as any,
                }}
                $style={{ width: '100%', height: '100%' }}
            />
        </div>
    )

    const isValueMulti = op ? op === OPERATOR.IN || op === OPERATOR.NOT_IN : false
    const isValueValid = op ? op !== OPERATOR.EXISTS || op !== OPERATOR.NOT_EXISTS : true
    const $attrs = [
        {
            type: 'property',
            value: property,
            options: $fieldOptions,
            renderer: FilterField,
            inputRef,
            valid: true,
            multi: false,
            optionFilter: isCurrentOptionMatch(0),
            onActive: () => {
                setInput('')
            },
        },
        {
            type: 'op',
            value: op,
            options: $operatorOptions,
            renderer: FilterOperator,
            inputRef,
            valid: true,
            multi: false,
            optionFilter: isCurrentOptionMatch(1),
            onActive: () => {
                setInput('')
            },
        },
        {
            type: 'value',
            value,
            options: $valueOptions,
            renderer: FilterFieldValue,
            inputRef,
            valid: isValueValid,
            multi: isValueMulti,
            onActive: () => {
                // trace('value active', { isValueMulti, value })
                // only single input value need to reset input value
                // if (isValueMulti) return
                // setInput(value)
                setInput('')
            },
            // input used for value not for search when type == value
            optionFilter: () => true,
        },
    ].filter((item) => item.valid)

    const attrs = $attrs.map((item, index) => {
        return {
            ...item,
            isEditing: focusTarget === index && focused,
            value: item.value,
            renderInput: () => Input,
            renderAfter: () => (index === $attrs.length - 1 ? Remove : undefined),
            onChange: (v: any) => confirm(v, index),
            onClick: () => focusOnTarget(index),
        }
    })

    // trace('filter', { isFocus, focusTarget, property })

    // if target active trigger onActive
    useEffect(() => {
        attrs[focusTarget]?.onActive?.()
        // eslint-disable-next-line react-hooks/exhaustive-deps
    }, [focusTarget, isFocus])

    return (
        <div
            className={styles.filters}
            ref={ref}
            role='button'
            tabIndex={0}
            onKeyDown={handleKeyDown}
            onClick={handleClick}
            style={style}
        >
            {attrs.map(({ valid, type, ...other }) => {
                if (!other.renderer) return null
                return <other.renderer key={type} {...other} />
            })}
        </div>
    )
}
