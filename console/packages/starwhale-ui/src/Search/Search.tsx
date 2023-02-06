import { ColumnFilterModel, ColumnSchemaDesc } from '@starwhale/core/datastore'
import { createUseStyles } from 'react-jss'
import React, { useState, useRef, useEffect } from 'react'
import { useClickAway } from 'react-use'
import _ from 'lodash'
import FilterRenderer from './FilterRenderer'
import { ValueT } from './types'
import IconFont from '../IconFont'
import { LabelSmall } from 'baseui/typography'

export const useStyles = createUseStyles({
    searchBar: {
        'display': 'flex',
        'border': '1px solid #CFD7E6',
        'gap': '10px',
        'height': '32px',
        'lineHeight': '20px',
        'overflowX': 'auto',
        'alignItems': 'center',
        'padding': '4px',
        'borderRadius': '4px',
        '&::-webkit-scrollbar': {
            height: '4px !important',
        },

        'flexGrow': '0',
        'overflowY': 'hidden',
    },
    startIcon: {
        width: '34px',
        display: 'grid',
        placeItems: 'center',
        marginRight: '-10px',
    },
})

// @ts-ignore
const containsNode = (parent, child) => {
    return child && parent && parent.contains(child as any)
}

export interface ISearchProps {
    fields: ColumnSchemaDesc[]
    value?: ValueT[]
    onChange?: (args: ValueT[]) => void
}

export default function Search({ value = [], onChange, ...props }: ISearchProps) {
    const styles = useStyles()
    const ref = useRef<HTMLDivElement>(null)
    const [isEditing, setIsEditing] = useState(false)
    const [items, setItems] = useState<ValueT[]>(value)
    const [editingItem, setEditingItem] = useState<{ index: any; value: ValueT } | null>(null)

    useEffect(() => {
        if (_.isEqual(value, items)) return
        setItems(value ?? [])
    }, [value, items])

    useClickAway(ref, (e) => {
        if (containsNode(ref.current, e.target)) return
        setIsEditing(false)
    })

    const column = React.useMemo(() => {
        return new ColumnFilterModel(props.fields)
    }, [props.fields])

    const count = React.useRef(100)
    const filters = React.useMemo(() => {
        count.current += 1
        const tmps = items.map((item, index) => {
            return (
                <FilterRenderer
                    key={[index, item.property].join('-')}
                    index={index}
                    value={item}
                    isEditing={isEditing}
                    isDisabled={false}
                    isFocus={editingItem?.index === index}
                    column={column}
                    onClick={() => {
                        if (editingItem?.index !== index) setEditingItem({ index, value: item })
                    }}
                    // @ts-ignore
                    containerRef={ref}
                    onChange={(newValue: any) => {
                        let newItems = []
                        if (!newValue) {
                            newItems = items.filter((key, i) => i !== index)
                        } else {
                            newItems = items.map((tmp, i) => (i === index ? newValue : tmp))
                        }
                        newItems = newItems.filter((tmp) => tmp && tmp.property && tmp.op && tmp.value)
                        setItems(newItems)
                        onChange?.(newItems)
                        setEditingItem({ index: -1, value: {} })
                    }}
                />
            )
        })
        tmps.push(
            <FilterRenderer
                key={count.current}
                index={-1}
                value={{}}
                isEditing={isEditing}
                isDisabled={false}
                isFocus={editingItem ? editingItem.index === -1 : false}
                column={column}
                style={{ flex: 1 }}
                onClick={() => {
                    if (editingItem?.index !== -1) setEditingItem({ index: -1, value: {} })
                }}
                // @ts-ignore
                containerRef={ref}
                onChange={(newValue: any) => {
                    let newItems = [...items]
                    // remove prev item
                    if (!newValue) {
                        newItems.splice(-1)
                    } else {
                        newItems.push(newValue)
                    }
                    newItems = newItems.filter((tmp) => tmp && tmp.property && tmp.op && tmp.value)
                    setItems(newItems)
                    onChange?.(newItems)
                }}
            />
        )
        return tmps
    }, [items, isEditing, column, editingItem, onChange])

    return (
        <div
            role='button'
            tabIndex={0}
            className={styles.searchBar}
            ref={ref}
            style={{ borderColor: isEditing ? '#799EE8' : '#CFD7E6' }}
            onKeyDown={(e: React.BaseSyntheticEvent) => {
                if (e.target.classList.contains('filter-remove')) return
                setIsEditing(true)
            }}
            onClick={(e: React.BaseSyntheticEvent) => {
                if (e.target.classList.contains('filter-remove')) return
                setIsEditing(true)
            }}
        >
            <div className={styles.startIcon}>
                <IconFont type='filter' size={12} kind='gray' />
            </div>
            {!isEditing && items.length === 0 && (
                <LabelSmall $style={{ color: 'rgba(2,16,43,0.40)', position: 'absolute', left: '37px' }}>
                    Search and Filter
                </LabelSmall>
            )}
            {filters}
        </div>
    )
}
