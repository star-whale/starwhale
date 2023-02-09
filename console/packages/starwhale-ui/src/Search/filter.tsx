import React, { useState, useEffect } from 'react'
import { PLACEMENT, Popover } from 'baseui/popover'
import { FilterT, Operators } from './types'
import { createUseStyles } from 'react-jss'
import { StatefulFilterMenu } from './StatefulFilterMenu'

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
    },
    labelEmpty: {
        display: 'flex',
        height: '22px',
        lineHeight: '22px',
        position: 'relative',
    },
})
function PopoverContainer(props: {
    onItemSelect?: (props: { item: { label: string; type: string } }) => void
    options: any[]
    isOpen: boolean
    children: React.ReactNode
    inputRef?: React.RefObject<HTMLInputElement>
    value?: any
}) {
    const [isOpen, setIsOpen] = useState(false)
    const styles = useStyles()

    useEffect(() => {
        setIsOpen(props.isOpen)
    }, [props.isOpen])

    const handleClose = () => setIsOpen(false)

    return (
        <Popover
            placement={PLACEMENT.bottomLeft}
            isOpen={isOpen}
            overrides={{
                Body: {
                    style: {
                        marginTop: '32px',
                    },
                },
            }}
            content={() => (
                <StatefulFilterMenu
                    // @ts-ignore cascade keydown event with input
                    keyboardControlNode={props.inputRef}
                    items={props.options}
                    onItemSelect={(args) => {
                        // @ts-ignore
                        props.onItemSelect?.(args)
                        handleClose()
                    }}
                    overrides={{
                        List: {
                            props: {
                                className: 'filter-popover',
                            },
                            style: {
                                minHeight: '150px',
                                minWidth: '150px',
                                maxHeight: '500px',
                                // overflow: 'auto',
                            },
                        },
                    }}
                />
            )}
        >
            <p
                className={props.children ? styles.label : styles.labelEmpty}
                title={typeof props.value === 'string' ? props.value : ''}
            >
                {props.children}
            </p>
        </Popover>
    )
}

function Filter(options: FilterT): FilterT {
    const operatorOptions = options.operators.map((key: string) => {
        const operator = Operators[key]
        return {
            id: operator.key,
            type: operator.key,
            label: operator.label,
        }
    })

    return {
        kind: options.kind,
        operators: options.operators,
        renderField: function RenderField({ options: renderOptions = [], isEditing = false, ...rest }) {
            return (
                <PopoverContainer
                    {...rest}
                    options={renderOptions}
                    isOpen={isEditing}
                    onItemSelect={({ item }) => rest.onChange?.(item.type)}
                >
                    {typeof rest.value === 'string' ? normalize(rest.value) : rest.value}
                </PopoverContainer>
            )
        },
        renderOperator: function RenderOperator({ isEditing = false, ...rest }) {
            return (
                <PopoverContainer
                    {...rest}
                    options={operatorOptions}
                    isOpen={isEditing}
                    onItemSelect={({ item }) => rest.onChange?.(item.type)}
                >
                    {operatorOptions.find((v) => v.type === rest.value)?.label ?? ''}
                </PopoverContainer>
            )
        },
        renderValue: options?.renderValue ?? undefined,
        // renderFieldValue: options?.renderFieldValue ?? <></>,
    }
}
export default Filter
