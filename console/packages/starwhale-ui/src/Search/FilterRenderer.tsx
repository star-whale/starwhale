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
    onBlur = () => {},
    onRemove = () => {},
    isFocus = false,
    isEditing = false,
    style = {},
    fields,
    ...rest
}: FilterPropsT & {
    fields: SearchFieldSchemaT[]
    style?: React.CSSProperties
    onClick?: () => void
    containerRef?: React.RefObject<HTMLDivElement>
}) {
    const [input, setInput] = useState<any>('')
    const styles = useStyles()
    const ref = useRef<HTMLDivElement>(null)
    const inputRef = useRef<HTMLInputElement>(null)
    const $columns = fields

    const [cached, setCached] = useState<any>({})
    // only rawValues all exist then cached
    useEffect(() => {
        if (isValueExist(rawValues.property) && isValueExist(rawValues.op) && isValueExist(rawValues.value)) {
            setCached(rawValues)
        }
    }, [rawValues])

    const origins = React.useMemo(
        () => [
            { type: 'property', value: rawValues.property },
            { type: 'op', value: rawValues.op },
            { type: 'value', value: rawValues.value },
        ],
        [rawValues]
    )
    const [machine, send, service] = useMachine(filterMachine, {
        context: {
            origins: cached,
            values: origins,
        },
    })
    const { values, focusTarget, focused } = machine.context
    const property = values[0]?.value
    const op = values[1]?.value
    const value = values[2]?.value

    console.log('[filter]:', values, rawValues, cached)

    const { FilterOperator, FilterField, FilterValue, FilterFieldValue, filter } = useMemo(() => {
        const field = $columns.find((tmp) => tmp.name === property)
        const filter = dataStoreToFilter(field?.type as DataTypeT)()
        return {
            filter,
            FilterOperator: filter.renderOperator,
            FilterField: filter.renderField,
            FilterFieldValue: filter.renderFieldValue,
            FilterValue: filter.renderValue,
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
        return $columns.filter(isCurrentOptionMatch(0)).map((tmp) => {
            return {
                id: tmp.path,
                type: tmp.path,
                label: tmp.label,
            }
        })
    }, [$columns, isCurrentOptionMatch])

    const $operatorOptions = React.useMemo(() => {
        return filter.operators
            .map((key: string) => {
                const operator = Operators[key]
                return {
                    id: operator.key,
                    type: operator.key,
                    label: operator.label,
                }
            })
            .filter(isCurrentOptionMatch(1))
    }, [filter, isCurrentOptionMatch])

    const $valueOptions = React.useMemo(() => {
        const column = $columns.find((tmp) => tmp.name === property)
        if (!column) return []
        return column
            .getHints()
            ?.map((tmp) => {
                return {
                    id: tmp,
                    type: tmp,
                    label: tmp,
                }
            })
            .filter(isCurrentOptionMatch(2))
    }, [$columns, property, isCurrentOptionMatch])

    const handleInputChange = (event: React.SyntheticEvent<HTMLInputElement> | any) => {
        if (typeof event === 'object' && 'target' in event) {
            setInput((event.target as any).value)
        } else {
            setInput(event ?? '')
        }
        focus()
    }

    const handleKeyDown = (event: KeyboardEvent) => {
        switch (event.keyCode) {
            case 27:
                reset()
                break
            case 9: // tab
            case 13: // enter
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

    // esc
    const reset = () => {
        send({ type: 'RESET' })
        // curr blur then select the lastest one
        onBlur()
    }
    const submit = () => {
        const hasSearchInput = isValueExist(input)
        if (!property) return
        if (!op) return
        // has input then reset default value
        if (hasSearchInput) confirm(input, 2)
        // has no input then use default value
        if (value) confirm(value, 2)
    }

    const blur = () => {
        send({ type: 'BLUR' })
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

    const confirm = (v, index) => {
        setInput('')
        send({ type: 'CONFIRM', value: v, index, callback: onSubmit })
    }

    const focusRemove = () => {
        const hasSearchInput = isValueExist(input)
        if (hasSearchInput) return

        send({ type: 'REMOVE', index: focusTarget })
    }

    const onSubmit = (tmp) => {
        onChange(tmp)
        blur()
    }

    const handleClick = () => {
        rest.onClick?.()
        focus()
    }

    const fieldDropdownRef = useRef(null)
    const opDropdownRef = useRef(null)

    // reset to raw status
    useClickAway(ref, (e) => {
        if (containsNode(fieldDropdownRef.current, e.target)) return
        if (containsNode(opDropdownRef.current, e.target)) return
        if (containsNode(document.querySelector('.filter-popover'), e.target)) return
        blur()
    })

    // keep focus by parent component
    useEffect(() => {
        if (isFocus && isEditing) {
            focusOnLastEdit()
        } else {
            blur()
        }
        // eslint-disable-next-line react-hooks/exhaustive-deps
    }, [isFocus, isEditing])

    useEffect(() => {
        const subscription = service.subscribe((curr) => {
            const ctx = curr.context
            console.log('>>>>>>>>>>>>>', curr.value, curr.event, property, op, value, {
                focusTarget: ctx.focusTarget,
                focused: ctx.focused,
            })

            switch (curr.value.editing) {
                case 'preRemove':
                    if (curr.event.type === 'REMOVE') {
                        onRemove()
                    }
                    break
                default:
                    break
            }
        })
        return () => subscription.unsubscribe()
    }, [service, send])

    const Remove = (
        <div className={styles.label}>
            {/* {Array.isArray(item.value) ? item.value.join(', ') : item.value} */}
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
            className='autosize-input inline-block relative flex-1'
            style={{
                minWidth: focused ? '50px' : 0,
                flexBasis: focused ? '100px' : 0,
                width: focused ? '160px' : 0,
                maxWidth: '100%',
                height: '100%',
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

    const $attrs = [
        {
            type: 'property',
            value: property,
            options: $fieldOptions,
            renderer: FilterField,
            inputRef,
            valid: () => true,
        },
        { type: 'op', value: op, options: $operatorOptions, renderer: FilterOperator, inputRef, valid: () => true },
        {
            type: 'value',
            value,
            options: $valueOptions,
            renderer: FilterFieldValue,
            inputRef,
            valid: () => (op ? op !== OPERATOR.EXISTS || op !== OPERATOR.NOT_EXISTS : true),
        },
    ].filter((item) => item.valid())

    const attrs = $attrs.map((item, index) => {
        return {
            ...item,
            isEditing: focusTarget === index && focused,
            value: focusTarget === index && focused ? undefined : item.value,
            renderInput: () => Input,
            renderAfter: () => (index === $attrs.length - 1 ? Remove : undefined),
            // after: index === attrs.length - 1 && editing ? <Remove /> : undefined,
            onChange: (v: any) => confirm(v, index),
            onClick: () => focusOnTarget(index),
        }
    })

    console.log('[filter]: ', {
        focused,
        focusTarget,
        input,
    })

    return (
        // @ts-ignore
        <div
            className={styles.filters}
            ref={ref}
            role='button'
            tabIndex={0}
            // @ts-ignore
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
