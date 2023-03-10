import React, { useState, useRef, useEffect } from 'react'
import { useClickAway } from 'react-use'
import _ from 'lodash'
import SelectorItemRender from './SelectorItemRender'
import { DynamicSelectorPropsT, SelectorItemPropsT, SelectorItemValueT } from './types'
import {
    defalutPlaceholder,
    defaultStartEnhancer,
    Placeholder,
    SelectorContainer,
    SelectorItemContainer,
    StartEnhancer,
} from './StyledComponent'
import SelectorPopover from './SelectorPopover'
import Tree from '../Tree/Tree'
import { KEY_STRINGS } from 'baseui/menu'

// @ts-ignore
const containsNode = (parent, child) => {
    return child && parent && parent.contains(child as any)
}
const isValueExist = (value: any) => {
    if (value === 0) return true
    return !!value
}
function SelectorItemByTree({ $isEditing, value, onChange, search, inputRef }: SelectorItemPropsT) {
    console.log('render', value)

    return (
        <SelectorPopover
            isOpen={$isEditing}
            content={
                <Tree
                    selectedIds={value}
                    onSelectedIdsChange={onChange}
                    search={search}
                    searchable={false}
                    multiple
                    keyboardControlNode={inputRef as any}
                />
            }
        />
    )
}

export function DynamicSelector<T = any>({
    onChange,
    startEnhancer = defaultStartEnhancer,
    placeholder = 'Select Item',
    options = [],
    ...rest
}: DynamicSelectorPropsT<T>) {
    const ref = useRef<HTMLDivElement>(null)
    const [isEditing, setIsEditing] = useState(false)
    const [values, setValues] = useState<SelectorItemValueT[]>(rest.value ?? [])
    const [editingIndex, setEditingIndex] = useState<number>(-1)
    const itemRefs = useRef<{
        [key: string]: {
            inputRef: { current: null | HTMLInputElement }
        }
    }>({})

    // useEffect(() => {
    //     if (_.isEqual(values, rest.value)) return
    //     setValues(rest.value ?? [])
    // }, [values, rest.value])

    useClickAway(ref, (e) => {
        if (containsNode(ref.current, e.target)) return
        if (containsNode(document.querySelector('.popover'), e.target)) return
        setIsEditing(false)
    })

    function focusItem(index: number) {
        if (itemRefs.current[index]) {
            itemRefs.current[index]?.inputRef?.current?.focus()
        }
    }

    const handleReset = () => {
        setIsEditing(false)
    }

    const handleClick = (index: number) => {
        if (editingIndex !== index) setEditingIndex(editingIndex)
    }

    const handelRemove = (index: number) => {
        const newValues = values.filter((key, i) => i !== index)
        setValues(newValues)
        onChange?.(newValues)
    }

    const handleEdit = (e: React.BaseSyntheticEvent) => {
        if (e.target.classList.contains('label-remove')) return
        setIsEditing(true)
        focusItem(values.length)
    }

    const handleAddItemRef = (index: number, itemRef: any) => {
        if (!itemRef) return
        itemRefs.current[index] = itemRef
    }

    const handleKeyDown = (event: KeyboardEvent) => {
        switch (event.key) {
            case KEY_STRINGS.Escape:
                handleReset()
                break
            default:
                handleEdit(event as any)
                break
        }
    }

    const count = React.useRef(100)
    const $values = React.useMemo(() => {
        count.current += 1
        const tmps =
            values?.map((value, index) => {
                return (
                    <SelectorItemRender
                        key={index}
                        value={value}
                        options={options}
                        isEditing={isEditing}
                        isFocus={editingIndex === index}
                        onRemove={() => handelRemove(index)}
                        onClick={() => handleClick(index)}
                        addItemRef={(ref: any) => handleAddItemRef(index, ref)}
                        onKeyDown={handleKeyDown}
                        // @ts-ignore
                        containerRef={ref}
                        onChange={(newValue: any) => {
                            let newValues = []
                            if (!newValue) {
                                newValues = values.filter((key, i) => i !== index)
                            } else {
                                newValues = values.map((tmp, i) => (i === index ? newValue : tmp))
                            }
                            newValues = newValues.filter(
                                (tmp) => tmp && tmp.property && tmp.op && isValueExist(tmp.value)
                            )
                            setValues(newValues)
                            onChange?.(newValues)
                            setEditingIndex(-1)
                        }}
                    />
                )
            }) ?? []

        const lastIndex = values.length
        tmps.push(
            <SelectorItemRender
                // key={count.current}
                key={lastIndex}
                options={options}
                value={{}}
                isEditing={isEditing}
                isFocus={editingIndex === lastIndex}
                style={{ flex: 1 }}
                onClick={() => () => handleClick(lastIndex)}
                addItemRef={(ref: any) => handleAddItemRef(lastIndex, ref)}
                onKeyDown={handleKeyDown}
                // @ts-ignore
                containerRef={ref}
                onChange={(newValue: any) => {
                    const newValues = [...values]
                    // remove prev item
                    if (!newValue) {
                        newValues.splice(-1)
                    } else {
                        newValues.push(newValue)
                    }
                    // newValues = newValues.filter((tmp) => tmp && tmp.property && tmp.op && isValueExist(tmp.value))
                    setValues(newValues)
                    onChange?.(newValues)
                }}
            />
        )

        return tmps
    }, [values, isEditing, options, onChange, editingIndex, itemRefs])

    const shareProps = {
        $isEditing: isEditing,
    }

    return (
        <SelectorContainer role='button' tabIndex={0} ref={ref} onKeyDown={handleKeyDown} onClick={handleEdit}>
            <StartEnhancer {...shareProps}>
                {typeof startEnhancer === 'function' ? startEnhancer() : startEnhancer}
            </StartEnhancer>
            <Placeholder>{!isEditing && values.length === 0 && defalutPlaceholder(placeholder)}</Placeholder>
            <SelectorItemContainer>{$values}</SelectorItemContainer>
        </SelectorContainer>
    )
}

export default (props: DynamicSelectorPropsT<any>) => {
    const options = [
        {
            id: '',
            data: {},
            getDataToLabel: (data: any) => data?.label,
            getDataToValue: (data: any) => data?.id,
            render: SelectorItemByTree as React.FC<any>,
        },
    ]
    return <DynamicSelector {...props} options={options} />
}
