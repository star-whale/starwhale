import { ColumnFilterModel, DataTypeT } from '@starwhale/core/datastore'
import React, { useEffect, useMemo, useRef, useState } from 'react'
import { useClickAway } from 'react-use'
import AutosizeInput from '../base/select/autosize-input'
import { FilterPropsT, ValueT } from '../Search/types'
// import { FilterPropsT, ValueT } from './types'
// import { dataStoreToFilter } from './utils'
import {
    AutosizeInputContainer,
    defaultLabelRemoveIcon,
    LabelContainer,
    LabelRemove,
    SelectItemContainer,
} from './StyledComponent'

// @ts-ignore
const containsNode = (parent, child) => {
    return child && parent && parent.contains(child as any)
}

const isValueExist = (value: any) => {
    if (value === 0) return true
    return !!value
}

export function SelectorItem({
    value: rawValues = {},
    items = [],
    onChange = () => {},
    isFocus = false,
    isEditing = false,
    style = {},
    ...rest
}: FilterPropsT & {
    column: ColumnFilterModel
    style?: React.CSSProperties
    onClick?: () => void
    containerRef?: React.RefObject<HTMLDivElement>
}) {
    const [values, setValues] = useState<ValueT>(rawValues)
    const [value, setValue] = useState<any>(rawValues?.value)
    const [editing, setEditing] = useState(false)
    const [removing, setRemoving] = useState(false)
    const ref = useRef<HTMLDivElement>(null)
    const inputRef = useRef<HTMLInputElement>(null)

    const handleInputChange = (event: React.SyntheticEvent<HTMLInputElement> | any) => {
        if (typeof event === 'object' && 'target' in event) {
            setValue((event.target as any).value)
        } else {
            setValue(event ?? '')
        }
    }

    const handleReset = () => {
        setValue(values.value)
        setRemoving(false)
        setEditing(false)
    }

    const handleKeyDown = (event: KeyboardEvent) => {
        // const valueExists = isValueExist(value)
        // switch (event.keyCode) {
        //     case 27:
        //         handleReset()
        //         break
        //     case 9: // tab
        //     case 13: // enter
        //         if (valueExists && op && property) {
        //             const newValues = {
        //                 value,
        //                 op,
        //                 property,
        //             }
        //             setValues(newValues)
        //             onChange?.(newValues)
        //             setEditing(false)
        //         }
        //         break
        //     case 8: // backspace
        //         event.stopPropagation()
        //         if (removing && !valueExists) {
        //             // first remove op
        //             if (op) {
        //                 setOp(undefined)
        //                 return
        //             }
        //             // second remove property
        //             if (property) setProperty(undefined)
        //             setRemoving(false)
        //             // remove prev item when there is no label value to delete
        //             if (!op && !property && !valueExists) onChange?.(undefined)
        //         }
        //         if (!valueExists) {
        //             setRemoving(true)
        //         }
        //         break
        //     default:
        //         break
        // }
    }

    const handleFocus = () => {
        rest.onClick?.()
        setEditing(true)
        inputRef.current?.focus()
    }

    const fieldDropdownRef = useRef(null)
    const opDropdownRef = useRef(null)

    // reset to raw status
    useClickAway(ref, (e) => {
        if (containsNode(fieldDropdownRef.current, e.target)) return
        if (containsNode(opDropdownRef.current, e.target)) return
        if (containsNode(document.querySelector('.popover'), e.target)) return

        handleReset()
    })

    // keep focus when editing
    // useEffect(() => {
    //     if (editing) {
    //         inputRef.current?.focus()
    //     }
    // }, [editing])

    // keep focus by parent component
    useEffect(() => {
        if (isFocus && isEditing) {
            setEditing(true)
            inputRef.current?.focus()
        }
    }, [isFocus, isEditing])

    // truncate values when first item is empty but with the same react key
    useEffect(() => {
        if (!rawValues.op && !rawValues.property && !rawValues.value) {
            setValues({})
            setValue(undefined)
        }
    }, [rawValues])

    const sharedProps = React.useMemo(
        () => ({
            $isEditing: editing,
            search: value,
            inputRef,
        }),
        [editing, value]
    )

    console.log(editing)

    const Item = React.useMemo(
        () =>
            items[0].getRender({
                ...sharedProps,
            }),
        [items, sharedProps]
    )

    return (
        <SelectItemContainer
            ref={ref}
            role='button'
            tabIndex={0}
            // @ts-ignore
            onKeyDown={handleKeyDown}
            onClick={handleFocus}
            style={style}
        >
            {/* {FilterField && (
                <FilterField
                    isEditing={editing && !property}
                    value={property as any}
                    onChange={(item: any) => {
                        setProperty(item)
                        setValue(undefined)
                        inputRef.current?.focus()
                    }}
                    options={$fieldOptions}
                    inputRef={inputRef as any}
                />
            )} */}
            {Item}
            {!editing && isValueExist(value) && (
                <LabelContainer title={value} className='label'>
                    {value}
                    <LabelRemove
                        className='filter-remove'
                        role='button'
                        onClick={(e) => {
                            e.preventDefault()
                            e.stopPropagation()
                            onChange?.(undefined)
                        }}
                        tabIndex={0}
                    >
                        {defaultLabelRemoveIcon(sharedProps)}
                    </LabelRemove>
                </LabelContainer>
            )}
            <AutosizeInputContainer className='autosize-input' {...sharedProps}>
                {/* @ts-ignore */}
                <AutosizeInput
                    inputRef={inputRef as any}
                    value={value}
                    onChange={handleInputChange}
                    // overrides={{ Input: FilterValue as any }}
                    // overrides={{ Input: FilterValue as any }}
                    $style={{ width: '100%', height: '100%' }}
                />
            </AutosizeInputContainer>
        </SelectItemContainer>
    )
}

export default SelectorItem
