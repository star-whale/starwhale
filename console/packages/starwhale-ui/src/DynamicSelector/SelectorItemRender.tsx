import React, { RefObject, useEffect, useMemo, useRef, useState } from 'react'
import { useClickAway } from 'react-use'
import AutosizeInput from '../base/select/autosize-input'
import {
    AutosizeInputContainer,
    defaultLabelRemoveIcon,
    LabelContainer,
    LabelRemove,
    LabelsContainer,
    SelectItemContainer,
} from './StyledComponent'
import { SelectorItemRenderPropsT } from './types'
import SelectorPopover from './SelectorPopover'

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
        data,
        ...rest
    }: SelectorItemRenderPropsT,
    itemRef: RefObject<any>
) {
    const [selectedIds, setSelectedIds] = React.useState<string[]>([])
    const [search, setSearch] = useState<any>()
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

    const sharedProps = React.useMemo(
        () => ({
            $isEditing: isEditing && isFocus,
            $isFocus: isFocus,
            $isGrid: true,
            search,
        }),
        [isEditing, isFocus, search]
    )

    const handleChange = (ids: any) => {
        setSelectedIds(ids)
        onChange?.(ids)
    }

    const handleRemove = (id: any) => {
        const newIds = selectedIds.filter((item) => item !== id)
        setSelectedIds(newIds)
        onChange?.(newIds as any)
    }

    const handleReset = () => {
        setRemoving(false)
    }

    const handleKeyDown = (event: KeyboardEvent) => {
        const valueExists = isValueExist(value)
        switch (event.keyCode) {
            case 27:
                handleReset()
                break
            case 9: // tab
            case 13: // enter
                break
            case 8: // backspace
                event.stopPropagation()
                if (removing && !valueExists) {
                    setRemoving(false)
                }
                if (!valueExists) {
                    setRemoving(true)
                }
                if (removing) {
                    handleRemove(selectedIds[selectedIds.length - 1])
                }
                break
            default:
                break
        }
    }

    const getLabel = React.useCallback(
        (label: string, id: number | string) => {
            return (
                <LabelContainer key={id} title={label} className='label'>
                    {label}
                    <LabelRemove className='label-remove' role='button' onClick={() => handleRemove(id)} tabIndex={0}>
                        {defaultLabelRemoveIcon()}
                    </LabelRemove>
                </LabelContainer>
            )
        },
        // eslint-disable-next-line react-hooks/exhaustive-deps
        [selectedIds, onChange]
    )

    const $selectedLabels = React.useMemo(() => {
        return selectedIds.map((id) => {
            const itemData = itemOption.getData(itemOption.info, id)
            const label = itemOption.getDataToLabel(itemData)
            return getLabel(label, id)
        })
    }, [selectedIds, itemOption, getLabel])

    // const $valueLabels = React.useMemo(() => {
    //     if (isEditing && !isValueExist(value)) return
    // }, [selectedIds, itemOption, getLabel])

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

    // reset to raw status
    useClickAway(ref, (e) => {
        if (containsNode(document.querySelector('.popover'), e.target)) return
        handleReset()
    })

    // keep focus by parent component
    React.useEffect(() => {
        if (isFocus && isEditing) {
            inputRef.current?.focus()
        }
    }, [isFocus, isEditing])

    return (
        <SelectItemContainer
            ref={ref}
            role='button'
            tabIndex={0}
            // @ts-ignore
            onKeyDown={handleKeyDown}
            style={style}
            {...sharedProps}
        >
            <LabelsContainer {...sharedProps}>{$selectedLabels}</LabelsContainer>
            <SelectorPopover
                isOpen={sharedProps.$isEditing}
                rows={Math.ceil($selectedLabels.length / 2)}
                content={
                    <itemOption.render
                        {...sharedProps}
                        value={selectedIds}
                        onChange={handleChange}
                        inputRef={inputRef}
                        info={itemOption.info}
                    />
                }
            />
            {/* {!isEditing && isValueExist(value) && (
                <LabelContainer title={value.value} className='label'>
                    {value}
                    <LabelRemove className='label-remove' role='button' onClick={onRemove} tabIndex={0}>
                        {defaultLabelRemoveIcon()}
                    </LabelRemove>
                </LabelContainer>
            )} */}
            {isEditing && (
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
            )}
        </SelectItemContainer>
    )
}

export default React.forwardRef(SelectorItemRender)
