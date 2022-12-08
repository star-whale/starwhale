import { ColumnModel } from '@starwhale/core/datastore'
import { InputContainer } from 'baseui/input/styled-components'
import { Popover } from 'baseui/popover'
import { SelectProps } from 'baseui/select'
import React, { useRef, useState } from 'react'
import AutosizeInput from '../base/select/autosize-input'
import Button from '../Button'
import Input from '../Input'
import Select from '../Select'
import { OPERATOR } from './constants'
import FilterString from './FilterString'
import { FilterPropsT, ValueT } from './types'

export default function FilterRenderer({
    value: rawValues = {},
    onChange = () => {},
    isDisabled = false,
    isEditing = false,
    fields = [],
}: FilterPropsT) {
    const [values, setValues] = useState<ValueT>(rawValues)
    const [value, setValue] = useState<any>(rawValues?.value)
    const [property, setProperty] = useState<string>(rawValues?.property ?? 'property')
    const [op, setOp] = useState<string>(rawValues?.op ?? '=')
    const [editing, setEditing] = useState(false)
    const [removing, setRemoving] = useState(false)

    const column = new ColumnModel(fields)

    const $columns = React.useMemo(() => {
        return column.getSearchColumns()
    }, [fields])

    const $fieldOptions = React.useMemo(() => {
        return $columns.map((column) => {
            return {
                id: column.path,
                label: column.label,
            }
        })
    }, [$columns])

    // const $operators = React.useMemo(() => {
    //     const kind = columns.find((column) => column.key === key)?.filterType
    //     if (kind && kind in FilterTypeOperators) {
    //         return FilterTypeOperators?.[kind].map((opV) => Operators[opV])
    //     }
    //     return FilterTypeOperators.sysDefault.map((opV) => Operators[opV])
    // }, [columns, key])

    // const OperatorSelector = filter
    const handleClick = () => {
        setEditing(!editing)
    }
    const handleClose = () => {
        setEditing(false)
    }

    const filter = FilterString()
    console.log(filter)
    const FilterValue = filter.renderFieldValue ?? <></>
    const FilterOperator = filter.renderOperator ?? <></>

    const handleKeyDown = (event: KeyboardEvent) => {
        console.log(event.keyCode, removing && !value, value, op, property)
        switch (event.keyCode) {
            case 13:
                onChange?.(values)
                break
            case 8: // backspace
                // event.preventDefault()
                event.stopPropagation()

                if (removing && !value) {
                    if (op) {
                        console.log('remove op')
                        setOp(undefined)
                        return
                    }
                    if (property) {
                        console.log('remove property')
                        setProperty(undefined)
                    }
                    // update values
                    // setValues({})
                    setRemoving(false)
                }

                if (!value) {
                    setRemoving(true)
                }

                break
        }
    }

    const backspaceCount = useRef(0)

    const handleInputChange = (e) => {
        // e.preventDefault()
        setValue(event.target.value)
    }

    const handleInit = () => {
        setProperty(values.property)
        setOp(values.op ?? '=')
        setValue(values.value)
        setRemoving(false)
        setEditing(false)
    }

    return (
        <div
            className='filter-ops'
            style={{ position: 'relative', display: 'flex', gridTemplateColumns: 'repeat(4,1fr)', gap: '8px' }}
            onKeyDown={handleKeyDown}
            onBlur={handleInit}
        >
            {/* <Popover
                isOpen={false}
                focusLock
                returnFocus
                content={() => {
                    return <></>
                }}
                onClickOutside={handleClose}
                onEsc={handleClose}
                ignoreBoundary
            > */}
            <Button onClick={handleClick}>{values.property}</Button>
            {/* </Popover> */}
            <div style={{ display: 'inline-block', width: '200px' }}>
                <Select
                    size='compact'
                    options={$fieldOptions}
                    placeholder='-'
                    clearable={false}
                    onChange={(params) => {
                        if (!params.option) {
                            return
                        }
                        setValues({ ...value, property: params.option?.id as string })
                        setProperty(params.option?.id as string)

                        // if (values.property && values.op) onChange?.({ ...value, property: params.option?.id as string })
                    }}
                    value={values.property ? [{ id: values.property }] : []}
                />
            </div>
            {property && <div style={{ display: 'inline-block', width: '200px' }}>{property} </div>}
            {op && (
                <div style={{ display: 'inline-block', width: '100px' }}>
                    <FilterOperator isEditing={editing && property && !op} value={op} onChange={setOp} />
                </div>
            )}
            <FilterValue isEditing={editing} value={values.value} onChange={(e) => setValue(event.target.value)} />

            <div
                style={{ minWidth: '100px', display: 'inline-block', maxWidth: '100%', position: 'relative', flex: 1 }}
            >
                <AutosizeInput value={value} $style={{ width: '100%' }} onChange={handleInputChange} />
            </div>

            {/* <Select
                size='compact'
                disabled={disabled}
                placeholder='='
                clearable={false}
                options={$operators.map((item) => ({
                    id: item.key,
                    label: item.label,
                }))}
                onChange={(params) => {
                    if (!params.option) {
                        return
                    }
                    onChange?.({ value, key, op: Operators?.[params.option.id as string] })
                }}
                value={op ? [{ id: op.key }] : []}
            />
            {!['exists', 'notExists'].includes(op.key as string) && (
                <Input
                    size='compact'
                    disabled={disabled}
                    placeholder=''
                    clearable={false}
                    onChange={(e: any) => {
                        onChange?.({ key, op, value: e.target.value as string })
                    }}
                    value={value}
                />
            )} */}
        </div>
    )
}
