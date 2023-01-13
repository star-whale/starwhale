import { ColumnFilterModel, ColumnSchemaDesc } from '@starwhale/core/datastore'
import { createUseStyles } from 'react-jss'
import React, { useState, useRef, useEffect } from 'react'
import { useClickAway } from 'react-use'
// eslint-disable-next-line import/no-cycle
import FilterRenderer from './FilterRenderer'
// eslint-disable-next-line import/no-cycle
import { ValueT } from './types'

export const useStyles = createUseStyles({
    searchBar: {
        'display': 'flex',
        'border': '1px solid #CFD7E6',
        'gap': '10px',
        'height': '36px',
        'overflowX': 'auto',
        'alignItems': 'center',
        'padding': '4px',
        'borderRadius': '4px',
        '&::-webkit-scrollbar': {
            height: '4px !important',
        },
    },
    filters: {
        'position': 'relative',
        'display': 'inline-flex',
        'flexWrap': 'nowrap',
        'gap': '1px',
        'cursor': 'pointer',
        'width': 'auto',
        'height': '24px',
        '&:hover $label': {
            backgroundColor: '#EDF3FF',
        },
    },
    label: {
        height: '24px',
        lineHeight: '24px',
        padding: '0 8px',
        background: '#EEF1F6',
        borderRadius: '4px',
        whiteSpace: 'nowrap',
        textOverflow: 'ellipsis',
        overflow: ' hidden',
        display: 'flex',
        alignItems: 'center',
    },
})

export interface ISearchProps {
    fields: ColumnSchemaDesc[]
    value?: ValueT[]
    onChange?: (args: ValueT[]) => void
}
const raw = [{}]

export default function Search({ value, onChange, ...props }: ISearchProps) {
    const styles = useStyles()
    const ref = useRef<HTMLDivElement>(null)

    const [isEditing, setIsEditing] = useState(false)

    const [items, setItems] = useState<ValueT[]>(value ?? (raw as any))

    useEffect(() => {
        const newItems = value && value.length > 0 ? [...value, ...(raw as any)] : (raw as any)
        setItems(newItems)
    }, [value])

    useClickAway(ref, () => {
        setIsEditing(false)
    })

    const column = React.useMemo(() => new ColumnFilterModel(props.fields), [props.fields])

    return (
        <div
            role='button'
            tabIndex={0}
            className={styles.searchBar}
            ref={ref}
            style={{ borderColor: isEditing ? '#799EE8' : '#CFD7E6' }}
            onKeyDown={(e) => {
                // @ts-ignore
                if (e.target.classList.contains('filter-remove')) return
                setIsEditing(true)
            }}
        >
            {items.map((item, index) => {
                return (
                    <FilterRenderer
                        key={[index, item.property].join('-')}
                        value={item}
                        isEditing={isEditing}
                        isDisabled={false}
                        isFocus={index === items.length - 1 && isEditing}
                        column={column}
                        style={{
                            flex: index === items.length - 1 ? 1 : undefined,
                        }}
                        // @ts-ignore
                        containerRef={ref}
                        onChange={(newValue: any) => {
                            let newItems = []
                            if (!value) {
                                newItems = items.filter((_, i) => i !== index)
                            } else {
                                newItems = items.map((tmp, i) => (i === index ? newValue : tmp))
                            }
                            if (newItems.length === 0) {
                                newItems = [{}]
                            }
                            setItems(newItems)

                            if (newValue && newValue.property && newValue.op && newValue.value) {
                                setItems([...newItems, {}])
                                setIsEditing(true)
                            }

                            onChange?.(newItems.filter((tmp) => tmp.property && tmp.op && tmp.value))
                        }}
                    />
                )
            })}
        </div>
    )
}
