import React, { useState, useEffect } from 'react'
import { PLACEMENT, Popover } from 'baseui/popover'
import { FilterT } from './types'
import { createUseStyles } from 'react-jss'
import { StatefulFilterMenu } from './StatefulFilterMenu'
import Checkbox from '../Checkbox'
import { useSelections } from 'ahooks'
import { useTrace } from '@starwhale/core'

const normalize = (v: string) => ['..', v.split('/').pop()].join('/')

export const useStyles = createUseStyles({
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
        position: 'relative',
        userSelect: 'none',
    },
    labelEmpty: {
        display: 'flex',
        height: '22px',
        lineHeight: '22px',
        position: 'relative',
        userSelect: 'none',
    },
})

function SingleSelectMenu(props: any) {
    return (
        <StatefulFilterMenu
            // @ts-ignore cascade keydown event with input
            keyboardControlNode={props.inputRef}
            items={props.options}
            onItemSelect={(args) => {
                // @ts-ignore
                props.onItemSelect?.(args)
                props.onClose()
            }}
            overrides={{
                List: {
                    props: {
                        className: 'filter-popover',
                    },
                    style: {
                        maxHeight: '500px',
                    },
                },
            }}
        />
    )
}

function MultiSelectMenu(props: any) {
    const items = React.useMemo(() => props.options.map((v) => v.id), [props.options])
    const defaultSelected = React.useMemo(() => {
        if (!props.value) return []
        if (Array.isArray(props.value)) return props.value
        return props.value?.split(',')
    }, [props.value])
    const { selected, isSelected, toggle } = useSelections(items, defaultSelected)
    const $items = React.useMemo(() => {
        const next = props.options.map((v) => {
            return {
                id: v.type,
                label: <Checkbox checked={isSelected(v.id)}>{v.label}</Checkbox>,
            }
        })
        next.push({
            id: 'submit',
            label: (
                <div className='text-center'>
                    <p className='font-bold'>Done</p>
                    <i style={{ fontSize: '12px' }}>(Ctrl + Enter)</i>
                </div>
            ),
        })

        return next
    }, [props.options, isSelected])

    const submit = () => {
        props.onClose()
        props.onItemIdsChange?.(selected)
    }

    const focus = () => {
        if (props.inputRef?.current) {
            props.inputRef.current.focus()
        }
    }

    const trace = useTrace('multi-select-menu')
    trace({ selected, defaultSelected })

    return (
        <StatefulFilterMenu
            // @ts-ignore cascade keydown event with input
            keyboardControlNode={props.inputRef}
            items={$items}
            handleEnterKey
            onItemSelect={({ item, event: e }) => {
                if (item.id === 'submit') {
                    submit()
                    return
                }

                if (props.inputRef?.current) {
                    setTimeout(() => {
                        focus()
                    }, 100)
                }
                toggle(item.id)
                // @ts-ignore
                if (e?.key === 'Enter') {
                    e?.stopPropagation()
                    // @ts-ignore
                    if (!e?.ctrlKey) return
                    submit()
                }
            }}
            overrides={{
                List: {
                    props: {
                        className: 'filter-popover',
                    },
                    style: {
                        minWidth: '150px',
                        maxHeight: '500px',
                    },
                },
            }}
        />
    )
}

function PopoverContainer(props: {
    multi?: boolean
    onItemSelect?: (props: { item: { label: string; type: string } }) => void
    onItemIdsChange?: (ids: string[]) => void
    options: any[]
    isOpen: boolean
    children: React.ReactNode
    inputRef?: React.RefObject<HTMLInputElement>
}) {
    const [isOpen, setIsOpen] = useState(false)
    const ref = React.useRef<HTMLElement>(null)

    useEffect(() => {
        setIsOpen(props.isOpen)
    }, [props.isOpen])

    const handleClose = () => ref.current && setIsOpen(false)

    return (
        <Popover
            placement={PLACEMENT.bottomLeft}
            isOpen={isOpen}
            innerRef={ref}
            overrides={{
                Body: {
                    style: {
                        marginTop: '32px',
                    },
                },
            }}
            content={() =>
                !props.multi ? (
                    <SingleSelectMenu {...props} onClose={handleClose} />
                ) : (
                    <MultiSelectMenu {...props} onClose={handleClose} />
                )
            }
        >
            <div>{props.children}</div>
        </Popover>
    )
}

function Label(props) {
    const styles = useStyles()
    if (!props.value) return null
    return (
        <div
            // eslint-disable-next-line
            role='button'
            tabIndex={0}
            onClick={props.onClick}
            className={props.children ? styles.label : styles.labelEmpty}
            title={typeof props.value === 'string' ? props.value : ''}
        >
            {props.children}
        </div>
    )
}

function Filter(options: FilterT): FilterT {
    return {
        kind: options.kind,
        operators: options.operators,
        renderField: function RenderField({
            options: renderOptions = [],
            optionFilter = () => true,
            isEditing = false,
            ...rest
        }) {
            return (
                <PopoverContainer
                    {...rest}
                    options={renderOptions.filter(optionFilter)}
                    isOpen={isEditing}
                    onItemSelect={({ item }) => rest.onChange?.(item.type)}
                >
                    {isEditing && rest.renderInput?.()}
                    {!isEditing && (
                        <Label {...rest}>{typeof rest.value === 'string' ? normalize(rest.value) : rest.value}</Label>
                    )}
                </PopoverContainer>
            )
        },
        renderOperator: function RenderOperator({
            options: renderOptions = [],
            optionFilter = () => true,
            isEditing = false,
            ...rest
        }) {
            return (
                <PopoverContainer
                    {...rest}
                    options={renderOptions.filter(optionFilter)}
                    isOpen={isEditing}
                    onItemSelect={({ item }) => rest.onChange?.(item.type)}
                >
                    {isEditing && rest.renderInput?.()}
                    {!isEditing && (
                        <Label {...rest}>
                            {renderOptions.find((v) => v.id === rest.value)?.label} {rest.renderAfter?.()}
                        </Label>
                    )}
                </PopoverContainer>
            )
        },
        renderFieldValue: function RenderField({
            options: renderOptions = [],
            optionFilter = () => true,
            isEditing = false,
            ...rest
        }) {
            return (
                <PopoverContainer
                    {...rest}
                    options={renderOptions.filter(optionFilter)}
                    // only option exsit will show popover
                    isOpen={isEditing}
                    onItemSelect={({ item }) => rest.onChange?.(item.type)}
                    onItemIdsChange={(ids = []) => rest.onChange?.(ids.join(','))}
                >
                    {isEditing && rest.renderInput?.()}
                    {!isEditing && (
                        <Label {...rest}>
                            {Array.isArray(rest.value) ? rest.value.join(',') : rest.value} {rest.renderAfter?.()}
                        </Label>
                    )}
                </PopoverContainer>
            )
        },
        renderValue: options?.renderValue ?? undefined,
        // renderFieldValue: options?.renderFieldValue ?? <></>,
    }
}
export default Filter
