import { ColumnFilterModel, DataTypeT } from '@starwhale/core/datastore'
import React, { useEffect, useMemo, useRef, useState } from 'react'
import AutosizeInput from '../base/select/autosize-input'
import { FilterPropsT, ValueT } from './types'
import { useClickAway } from 'react-use'
import { useStyles } from './Search'
import IconFont from '../IconFont'
import { dataStoreToFilter } from './constants'

export default function FilterRenderer({
    value: rawValues = {},
    onChange = () => {},
    isDisabled = false,
    isEditing = false,
    isFocus = false,
    style = {},
    column = new ColumnFilterModel([]),
    containerRef,
}: FilterPropsT & {
    column: ColumnFilterModel
    style: React.CSSProperties
    containerRef: React.RefObject<HTMLDivElement>
}) {
    const [values, setValues] = useState<ValueT>(rawValues)
    const [value, setValue] = useState<any>(rawValues?.value)
    const [property, setProperty] = useState<string | undefined>(rawValues?.property)
    const [op, setOp] = useState<string | undefined>(rawValues?.op)
    const [editing, setEditing] = useState(false)
    const [removing, setRemoving] = useState(false)
    const styles = useStyles()
    const ref = useRef<HTMLDivElement>(null)
    const inputRef = useRef<HTMLInputElement>(null)

    const $columns = React.useMemo(() => {
        return column.getSearchColumns()
    }, [column])

    const $fieldOptions = React.useMemo(() => {
        return $columns.map((column) => {
            return {
                id: column.path,
                type: column.path,
                label: column.label,
            }
        })
    }, [$columns])

    const { filter, FilterOperator, FilterField } = useMemo(() => {
        const field = $columns.find((field) => field.name === property)
        const filter = dataStoreToFilter(field?.type as DataTypeT)()
        const FilterValue = filter.renderFieldValue
        const FilterOperator = filter.renderOperator
        const FilterField = filter.renderField
        return {
            filter,
            FilterOperator,
            FilterField,
        }
    }, [property, $columns])

    const handleKeyDown = (event: KeyboardEvent) => {
        // console.log(event.keyCode, removing && !value, value, op, property)
        switch (event.keyCode) {
            case 13: // enter
                if (value && op && property) {
                    const newValues = {
                        value,
                        op,
                        property,
                    }
                    setValues(newValues)
                    onChange?.(newValues)
                    setEditing(false)
                }
                break
            case 8: // backspace
                event.stopPropagation()

                if (removing && !value) {
                    // first remove op
                    if (op) {
                        setOp(undefined)
                        return
                    }
                    // second remove property
                    if (property) setProperty(undefined)
                    // third trigger remove all
                    setRemoving(false)
                    onChange?.(undefined)
                }
                if (!value) {
                    setRemoving(true)
                }
                break
        }
    }

    const handleInputChange = (event: React.SyntheticEvent<HTMLInputElement>) => {
        setValue((event.target as any)?.value)
    }

    const handleReset = () => {
        setProperty(values?.property)
        setOp(values?.op)
        setValue(values.value)
        setRemoving(false)
        setEditing(false)
    }

    const handleFocus = () => {
        setEditing(true)
        inputRef.current?.focus()
    }

    const fieldDropdownRef = useRef(null)
    const opDropdownRef = useRef(null)

    // reset to raw status
    useClickAway(ref, (e) => {
        if (containsNode(fieldDropdownRef.current, e.target)) return
        if (containsNode(opDropdownRef.current, e.target)) return
        handleReset()
    })
    // keep focus when editing
    useEffect(() => {
        if (editing && op) {
            inputRef.current?.focus()
        }
    }, [editing, op])

    // keep focus by parent component
    useEffect(() => {
        if (isFocus) {
            setEditing(true)
            inputRef.current?.focus()
        }
    }, [isFocus, inputRef.current])

    // truncate values when first item is empty but with the same react key
    useEffect(() => {
        if (!rawValues.op && !rawValues.property && !rawValues.value) {
            setValues({})
            setOp(undefined)
            setProperty(undefined)
            setValue(undefined)
        }
    }, [rawValues])

    return (
        // @ts-ignore
        <div ref={ref} className={styles.filters} onKeyDown={handleKeyDown} onClick={handleFocus} style={style}>
            {FilterField && (
                <FilterField
                    isEditing={editing && !property}
                    value={property as any}
                    onChange={(item: any) => setProperty(item)}
                    options={$fieldOptions}
                    mountNode={document.body}
                    innerRef={fieldDropdownRef}
                />
            )}
            {FilterOperator && (
                <FilterOperator
                    isEditing={!!(editing && property && !op)}
                    value={op as any}
                    onChange={(item: any) => {
                        setOp(item)
                        inputRef.current?.focus()
                    }}
                    innerRef={opDropdownRef}
                    mountNode={document.body}
                />
            )}
            {/* <FilterValue isEditing={editing} value={values.value} onChange={(e) => setValue(event.target.value)} /> */}

            {!editing && value && (
                <div className={styles.label} title={value}>
                    {value}
                    <div
                        role='button'
                        onClick={(e) => {
                            console.log('button click')
                            e.preventDefault()
                            e.stopPropagation()
                            onChange?.(undefined)
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
            )}
            <div
                style={{
                    minWidth: editing ? '100px' : 0,
                    display: 'inline-block',
                    maxWidth: '100%',
                    position: 'relative',
                    flex: 1,
                    flexBasis: editing ? '100px' : 0,
                    width: editing ? '100%' : 0,
                    height: editing ? '100%' : 0,
                }}
            >
                {/* @ts-ignore */}
                <AutosizeInput
                    inputRef={inputRef as any}
                    value={value}
                    onChange={handleInputChange}
                    $style={{ width: '100%', height: '100%' }}
                />
            </div>
        </div>
    )
}
// @ts-ignore
const containsNode = (parent, child) => {
    return child && parent && parent.contains(child as any)
}
