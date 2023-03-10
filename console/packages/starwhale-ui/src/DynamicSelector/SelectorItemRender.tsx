import { ColumnFilterModel, DataTypeT } from '@starwhale/core/datastore'
import React, { RefObject, useEffect, useMemo, useRef, useState } from 'react'
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
import { SelectorItemRenderPropsT } from './types'

// @ts-ignore
const containsNode = (parent, child) => {
    return child && parent && parent.contains(child as any)
}

const isValueExist = (v: any) => {
    if (v.value === 0) return true
    return !!v.value
}

export function SelectorItemRender(
    {
        value = {},
        options = [],
        onChange = () => {},
        onRemove = () => {},
        isFocus = false,
        isEditing = false,
        style = {},
        addItemRef,
        ...rest
    }: SelectorItemRenderPropsT,
    itemRef: RefObject<any>
) {
    const [selectedIds, setSelectedIds] = React.useState<string[]>([])
    const [search, setSearch] = useState<any>()
    const [editing, setEditing] = useState(false)
    const [removing, setRemoving] = useState(false)
    const ref = useRef<HTMLDivElement>(null)
    const inputRef = useRef<HTMLInputElement>(null)

    const itemOption = useMemo(() => {
        const tmp = options.find((item) => item.id === value?.id)
        if (!tmp) return options[0]
        return tmp
    }, [options, value])

    const handleInputChange = (event: React.SyntheticEvent<HTMLInputElement> | any) => {
        if (typeof event === 'object' && 'target' in event) {
            setSearch((event.target as any).value)
        } else {
            setSearch(event ?? '')
        }
    }

    const handleReset = () => {
        setRemoving(false)
        setEditing(false)
    }

    const handleKeyDown = (event: KeyboardEvent) => {
        const valueExists = isValueExist(value)
        switch (event.keyCode) {
            case 27:
                handleReset()
                break
            case 9: // tab
            case 13: // enter
                // if (valueExists && op && property) {
                //     const newValues = {
                //         value,
                //         op,
                //         property,
                //     }
                //     setValues(newValues)
                //     onChange?.(newValues)
                //     setEditing(false)
                // }
                break
            case 8: // backspace
                event.stopPropagation()
                if (removing && !valueExists) {
                    // first remove op
                    // if (op) {
                    //     setOp(undefined)
                    //     return
                    // }
                    // second remove property
                    // if (property) setProperty(undefined)
                    setRemoving(false)
                    // remove prev item when there is no label value to delete
                    // if (!op && !property && !valueExists) onChange?.(undefined)
                }
                if (!valueExists) {
                    setRemoving(true)
                }
                break
            default:
                break
        }
    }

    // const handleFocus = () => {
    //     rest.onClick?.()
    //     setEditing(true)
    //     inputRef.current?.focus()
    // }

    const fieldDropdownRef = useRef(null)
    const opDropdownRef = useRef(null)

    // reset to raw status
    useClickAway(ref, (e) => {
        if (containsNode(fieldDropdownRef.current, e.target)) return
        if (containsNode(opDropdownRef.current, e.target)) return
        if (containsNode(document.querySelector('.popover'), e.target)) return

        handleReset()
    })

    // // keep focus when editing
    // useEffect(() => {
    //     if (editing && search) {
    //         setEditing(true)
    //         inputRef.current?.focus()
    //     }
    // }, [editing, search])

    // // keep focus by parent component
    useEffect(() => {
        if (isFocus && isEditing) {
            setEditing(true)
            inputRef.current?.focus()
        }
    }, [isFocus, isEditing])

    // // truncate values when first item is empty but with the same react key
    // useEffect(() => {
    //     if (!rawValues.op && !rawValues.property && !rawValues.value) {
    //         setValues({})
    //         setSearch(undefined)
    //     }
    // }, [rawValues])

    const sharedProps = React.useMemo(
        () => ({
            $isEditing: isEditing,
            $isFocus: isFocus,
            search,
        }),
        [isEditing, search]
    )

    console.log('selector render', selectedIds, itemRef, sharedProps, value)

    React.useImperativeHandle(itemRef, () => ({
        focus: () => {
            inputRef.current?.focus()
        },
    }))

    React.useLayoutEffect(() => {
        addItemRef?.({
            inputRef,
        })
    }, [addItemRef])

    return (
        <SelectItemContainer
            ref={ref}
            role='button'
            tabIndex={0}
            // @ts-ignore
            onKeyDown={handleKeyDown}
            // onClick={handleFocus}
            style={style}
        >
            <itemOption.render {...sharedProps} value={selectedIds} onChange={setSelectedIds} inputRef={inputRef} />
            {!isEditing && isValueExist(value) && (
                <LabelContainer title={value.value} className='label'>
                    {value}
                    <LabelRemove className='label-remove' role='button' onClick={onRemove} tabIndex={0}>
                        {defaultLabelRemoveIcon()}
                    </LabelRemove>
                </LabelContainer>
            )}
            <AutosizeInputContainer className='autosize-input' {...sharedProps}>
                {/* @ts-ignore */}
                <AutosizeInput
                    inputRef={inputRef as any}
                    value={search}
                    onChange={handleInputChange}
                    // overrides={{ Input: FilterValue as any }}
                    $style={{ width: '100%', height: '100%' }}
                />
            </AutosizeInputContainer>
        </SelectItemContainer>
    )
}

export default React.forwardRef(SelectorItemRender)
