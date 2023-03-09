import { ColumnFilterModel, ColumnSchemaDesc } from '@starwhale/core/datastore'
import React, { useState, useRef, useEffect } from 'react'
import { useClickAway } from 'react-use'
import _ from 'lodash'
import SelectorItem from './SelectorItem'
import { ValueT } from './types'
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

// @ts-ignore
const containsNode = (parent, child) => {
    return child && parent && parent.contains(child as any)
}
const isValueExist = (value: any) => {
    if (value === 0) return true
    return !!value
}

export interface ISearchProps {
    fields: ColumnSchemaDesc[]
    value?: ValueT[]
    onChange?: (args: ValueT[]) => void
}

export function DynamicSelector({
    value = [],
    onChange,
    startEnhancer = defaultStartEnhancer,
    placeholder = defalutPlaceholder('Select Item'),
    values: rawValues = [],
    items = [
        {
            id: '',
            data: {},
            getDataToLabel: (data: any) => data?.label,
            getDataToValue: (data: any) => data?.id,
            render: ({ $isEditing, value, onChange, search }) => (
                <SelectorPopover
                    isOpen={$isEditing}
                    content={() => <Tree selectedIds={value} search={search} onSelectedIdsChange={onChange} multiple />}
                />
            ),
        },
    ],
    ...props
}: ISearchProps) {
    const ref = useRef<HTMLDivElement>(null)
    const [isEditing, setIsEditing] = useState(false)
    const [values, setValues] = useState<ValueT[]>(value)
    const [editingItem, setEditingItem] = useState<{ index: any; value: ValueT } | null>(null)

    useEffect(() => {
        if (_.isEqual(values, rawValues)) return
        setValues(values ?? [])
    }, [values, rawValues])

    useClickAway(ref, (e) => {
        if (containsNode(ref.current, e.target)) return
        if (containsNode(document.querySelector('.popover'), e.target)) return
        setIsEditing(false)
    })
    console.log('editingItem', values, editingItem)

    const count = React.useRef(100)
    const $values = React.useMemo(() => {
        count.current += 1
        const tmps = values.map((value, index) => {
            return (
                <SelectorItem
                    value={value}
                    key={index}
                    items={items}
                    isEditing={isEditing}
                    isFocus={editingItem?.index === index}
                    onClick={() => {
                        if (editingItem?.index !== index) setEditingItem({ index, value })
                    }}
                    // @ts-ignore
                    containerRef={ref}
                    onChange={(newValue: any) => {
                        let newValues = []
                        if (!newValue) {
                            newValues = values.filter((key, i) => i !== index)
                        } else {
                            newValues = values.map((tmp, i) => (i === index ? newValue : tmp))
                        }
                        newValues = newValues.filter((tmp) => tmp && tmp.property && tmp.op && isValueExist(tmp.value))
                        setValues(newValues)
                        onChange?.(newValues)
                        setEditingItem({ index: -1, value: {} })
                    }}
                />
            )
        })
        tmps.push(
            <SelectorItem
                key={count.current}
                items={items}
                value={{}}
                isEditing={isEditing}
                isFocus={editingItem ? editingItem.index === -1 : false}
                style={{ flex: 1 }}
                onClick={() => {
                    console.log('editingItem', -1)
                    if (editingItem?.index !== -1) setEditingItem({ index: -1, value: {} })
                }}
                // @ts-ignore
                containerRef={ref}
                onChange={(newValue: any) => {
                    let newValues = [...values]
                    // remove prev item
                    if (!newValue) {
                        newValues.splice(-1)
                    } else {
                        newValues.push(newValue)
                    }
                    newValues = newValues.filter((tmp) => tmp && tmp.property && tmp.op && isValueExist(tmp.value))
                    setValues(newValues)
                    onChange?.(newValues)
                }}
            />
        )
        return tmps
    }, [values, isEditing, editingItem, items, onChange])

    const shareProps = {
        $isEditing: isEditing,
    }

    const onEdit = (e: React.BaseSyntheticEvent) => {
        if (e.target.classList.contains('filter-remove')) return
        setIsEditing(true)
    }

    return (
        <SelectorContainer role='button' tabIndex={0} ref={ref} onKeyDown={onEdit} onClick={onEdit}>
            <StartEnhancer {...shareProps}>
                {typeof startEnhancer === 'function' ? startEnhancer(shareProps) : startEnhancer}
            </StartEnhancer>
            <Placeholder>{!isEditing && values.length === 0 && placeholder}</Placeholder>
            <SelectorItemContainer>{$values}</SelectorItemContainer>
        </SelectorContainer>
    )
}

export default DynamicSelector
