import React, { useState, useEffect } from 'react'
import { PLACEMENT, Popover, PopoverProps } from 'baseui/popover'
import { createUseStyles } from 'react-jss'
import { StatefulFilterMenu } from './StatefulFilterMenu'
import Checkbox from '../Checkbox'
import { useSelections } from 'ahooks'
import { mergeOverride } from '../base/helpers/overrides'

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
    // const trace = useTrace('single-select-menu')
    // trace({ inputRef: props.inputRef })

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
            overrides={
                mergeOverride(
                    {
                        List: {
                            style: {
                                maxHeight: '500px',
                                maxWidth: '500px',
                            },
                        },
                    } as any,
                    props.overrides
                ) as any
            }
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
                label: (
                    <Checkbox
                        overrides={{
                            Label: {
                                style: {
                                    wordBreak: 'break-word',
                                },
                            },
                        }}
                        isFullWidth
                        checked={isSelected(v.id)}
                    >
                        {v.label}
                    </Checkbox>
                ),
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

    // const trace = useTrace('multi-select-menu')
    // trace({ selected, defaultSelected, inputRef: props.inputRef })

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
                    style: {
                        maxWidth: '500px',
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
    Content?: React.FC<any>
    mountNode?: HTMLElement
    popperOptions?: any
    overrides?: any
    contentOverrides?: any
    placement?: PopoverProps['placement']
    autoClose?: boolean
}) {
    const [isOpen, setIsOpen] = useState(false)
    const ref = React.useRef<HTMLElement>(null)

    const {
        Content = SingleSelectMenu,
        inputRef,
        mountNode,
        popperOptions,
        overrides,
        placement = PLACEMENT.bottomLeft,
        contentOverrides,
        autoClose,
        ...rest
    } = props

    useEffect(() => {
        setIsOpen(props.isOpen)
    }, [props.isOpen])

    const handleClose = () => ref.current && setIsOpen(false)

    return (
        <Popover
            ignoreBoundary={false}
            popperOptions={popperOptions}
            mountNode={mountNode}
            placement={placement}
            isOpen={isOpen}
            innerRef={ref}
            overrides={
                mergeOverride(
                    {
                        Body: {
                            props: {
                                className: 'filter-popover',
                            },
                            style: {
                                marginTop: '32px',
                            },
                        },
                    } as any,
                    overrides
                ) as any
            }
            content={
                <Content
                    overrides={contentOverrides}
                    {...rest}
                    inputRef={inputRef}
                    onClose={autoClose ? handleClose : () => {}}
                />
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

export { Label, PopoverContainer, SingleSelectMenu, MultiSelectMenu }
export default PopoverContainer
