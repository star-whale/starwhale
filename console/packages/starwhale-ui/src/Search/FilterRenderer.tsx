import { DataTypeT, OPERATOR } from '@starwhale/core/datastore'
import React, { useEffect, useMemo, useRef, useState } from 'react'
import { useClickAway } from 'react-use'
import AutosizeInput from '../base/select/autosize-input'
import { FilterPropsT, SearchFieldSchemaT, ValueT } from './types'
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
    // const [values, setValues] = useState<ValueT>(rawValues)
    const [input, setInput] = useState<any>('')
    // const [property, setProperty] = useState<string | undefined>(rawValues?.property)
    const [, setOp] = useState<string | undefined>(rawValues?.op)
    const [editing, setEditing] = useState(false)
    const [removing, setRemoving] = useState(false)
    const styles = useStyles()
    const ref = useRef<HTMLDivElement>(null)
    const inputRef = useRef<HTMLInputElement>(null)
    const $columns = fields

    const origins = React.useMemo(
        () => [
            { type: 'property', value: rawValues.property },
            { type: 'op', value: rawValues.op },
            { type: 'value', value: rawValues.value },
        ],
        [rawValues.property, rawValues.op, rawValues.value]
    )
    const [machine, send, service] = useMachine(filterMachine, {
        context: {
            origins,
            values: origins,
        },
    })
    const property = machine.context.values[0]?.value
    const op = machine.context.values[1]?.value
    const value = machine.context.values[2]?.value

    const $fieldOptions = React.useMemo(() => {
        return $columns
            .filter((tmp) => {
                try {
                    return tmp.label?.match(input)
                } catch {
                    return false
                }
            })
            .map((tmp) => {
                return {
                    id: tmp.path,
                    type: tmp.path,
                    label: tmp.label,
                }
            })
    }, [$columns, input])

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
            .filter((tmp) => {
                try {
                    return tmp.label?.match(input)
                } catch {
                    return false
                }
            })
    }, [$columns, property, input])

    const { FilterOperator, FilterField, FilterValue, FilterFieldValue } = useMemo(() => {
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

    const handleInputChange = (event: React.SyntheticEvent<HTMLInputElement> | any) => {
        if (typeof event === 'object' && 'target' in event) {
            setInput((event.target as any).value)
        } else {
            setInput(event ?? '')
        }
        focus()
    }

    const handleKeyDown = (event: KeyboardEvent) => {
        const valueExists = isValueExist(input)

        switch (event.keyCode) {
            case 27:
                reset()
                break
            case 9: // tab
            case 13: // enter
                if (valueExists && op && property) {
                    submit({
                        value: input,
                        op,
                        property,
                    })
                }
                break
            case 8: // backspace
                event.stopPropagation()
                if (!valueExists) {
                    focusRemove()
                }
                break
            default:
                break
        }
    }

    const { values, focusTarget, focused } = machine.context

    // esc
    const reset = () => {
        send({ type: 'RESET' })
    }
    const submit = (tmp) => {
        setInput('')
        blur()
        onChange?.(tmp)
    }

    const blur = () => {
        send({ type: 'BLUR' })
    }

    const focus = (index = 0) => {
        inputRef.current?.focus()
        send({ type: 'FOCUS', index })
    }

    const focusOnTarget = (index = 0) => {
        focus()
        send({ type: 'FOCUSTARGET', index })
    }

    const focusLatest = () => {
        const index = values.findLastIndex((v) => v.value)
        focusOnTarget(Math.max(index, 0))
    }

    const confirm = (v, index) => {
        send({ type: 'CONFIRM', value: v, index })
    }

    const focusRemove = () => {
        send({ type: 'REMOVE', index: focusTarget })
    }

    const onRemove = () => {
        onChange?.(undefined)
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
            console.log('------------')
            focusLatest()
        } else {
            blur()
        }
    }, [isFocus, isEditing])

    // truncate values when first item is empty but with the same react key
    // useEffect(() => {
    //     if (!rawValues.op && !rawValues.property && !rawValues.value) {
    //         setValues({})
    //         setOp(undefined)
    //         setProperty(undefined)
    //         setValue(undefined)
    //     }
    // }, [rawValues])

    useEffect(() => {
        const subscription = service.subscribe((curr) => {
            const ctx = curr.context
            console.log(curr.value, curr.event, property, op, value, {
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

    const Remove = () => (
        <div className={styles.label}>
            {/* {Array.isArray(item.value) ? item.value.join(', ') : item.value} */}
            <div
                className='filter-remove'
                role='button'
                onClick={(e) => {
                    e.preventDefault()
                    e.stopPropagation()
                    onRemove()
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

    const attrs = [
        {
            type: 'property',
            value: property,
            options: $fieldOptions,
            renderer: FilterField,
            inputRef,
            valid: () => true,
        },
        { type: 'op', value: op, renderer: FilterOperator, inputRef, valid: () => true },
        {
            type: 'value',
            value,
            options: $valueOptions,
            renderer: FilterFieldValue,
            inputRef,
            valid: () => (op ? op !== OPERATOR.EXISTS || op !== OPERATOR.NOT_EXISTS : true),
        },
    ]
        .filter((item) => item.valid())
        .map((item, index) => {
            return {
                ...item,
                isEditing: focusTarget === index && focused,
                // value: focusTarget === index ? undefined : item.value,
                // after: index === attrs.length - 1 && editing ? <Remove /> : undefined,
                onChange: (v: any) => {
                    confirm(v, index)
                },
                onClick: () => {
                    focusOnTarget(index)
                },
            }
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
            <div
                className='autosize-input inline-block relative flex-1'
                style={{
                    minWidth: focused ? '50px' : 0,
                    flexBasis: focused ? '100px' : 0,
                    width: focused ? '100%' : 0,
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
        </div>
    )
}
