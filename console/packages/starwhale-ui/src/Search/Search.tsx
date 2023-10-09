import { RecordSchemaT, isSearchColumns } from '@starwhale/core/datastore'
import { createUseStyles } from 'react-jss'
import React, { useState, useRef } from 'react'
import { useClickAway } from 'react-use'
import FilterRenderer from './FilterRenderer'
import { SearchFieldSchemaT, ValueT } from './types'
import IconFont from '../IconFont'
import { LabelSmall } from 'baseui/typography'
import useTranslation from '@/hooks/useTranslation'

export const useStyles = createUseStyles({
    searchBar: {
        'display': 'flex',
        'border': '1px solid #CFD7E6',
        'height': '32px',
        'lineHeight': '20px',
        'alignItems': 'center',
        'padding': '4px',
        'borderRadius': '4px',
        '&::-webkit-scrollbar': {
            height: '4px !important',
        },
        'flexGrow': '1',
        '&:hover': {
            borderColor: '#799EE8 !important',
        },
        'overflowX': 'auto',
        'overflowY': 'hidden',
        'backgroundColor': '#fff',
    },
    filters: {
        display: 'flex',
        gap: '10px',
        height: '32px',
        lineHeight: '20px',
        alignItems: 'center',
        flexGrow: '1',
    },
    startIcon: {
        width: '34px',
        display: 'grid',
        placeItems: 'center',
        flexShrink: 0,
    },
    placeholder: {
        'position': 'relative',
        'display': 'flex',
        'width': 0,
        'alignItems': 'center',
        '& > div': {
            width: '150px',
        },
    },
})

// @ts-ignore
const containsNode = (parent, child) => {
    return child && parent && parent.contains(child as any)
}
const isValueExist = (value: any) => {
    if (value === 0) return true
    return !!value
}

export interface ISearchProps {
    fields: SearchFieldSchemaT[]
    value?: ValueT[]
    onChange?: (args: ValueT[]) => void
}

function useSearchColumns(columnTypes: { name: string; type: string }[]) {
    const searchColumns = React.useMemo(() => {
        if (!columnTypes) return []
        const arr: SearchFieldSchemaT[] = []
        const columns = columnTypes.filter((column) => isSearchColumns(column.name))
        columns.forEach((column) => {
            arr.push({
                ...column,
                path: column.name,
                label: column.name,
            })
        })

        return arr
    }, [columnTypes])

    return searchColumns
}

//   [ { value: [ 'SUCCESS' ], op: 'IN', property: 'sys/job_status' } ]

export default function Search({ value = [], onChange, fields }: ISearchProps) {
    const styles = useStyles()
    const [t] = useTranslation()
    const ref = useRef<HTMLDivElement>(null)
    const [isEditing, setIsEditing] = useState(false)
    const [editingItem, setEditingItem] = useState<{ index: any } | null>(null)

    const items = value

    useClickAway(ref, (e) => {
        if (containsNode(ref.current, e.target)) return
        if (containsNode(document.querySelector('.filter-popover'), e.target)) return
        setIsEditing(false)
    })

    console.log('[Search]: ', editingItem, value[0], value[1], value)

    const count = React.useRef(100)
    const filters = React.useMemo(() => {
        count.current += 1
        const tmps = items.map((item, index) => {
            return (
                <FilterRenderer
                    key={[index, item.property].join('-')}
                    value={item}
                    isEditing={isEditing}
                    isDisabled={false}
                    isFocus={editingItem?.index === index}
                    fields={fields}
                    onClick={() => {
                        if (editingItem?.index !== index) setEditingItem({ index })
                    }}
                    // @ts-ignore
                    containerRef={ref}
                    onRemove={() => {
                        const next = [...items]
                        next.splice(index, 1)
                        onChange?.(next)
                        console.log('next: ', index, next)
                        setEditingItem({ index: next.length - 1 < 0 ? 0 : next.length - 1 })
                    }}
                    onChange={(newValue: any) => {
                        let newItems: any[] = []
                        if (!newValue) {
                            newItems = items.filter((key, i) => i !== index)
                        } else {
                            newItems = items.map((tmp, i) => (i === index ? newValue : tmp))
                        }
                        newItems = newItems.filter((tmp) => tmp && tmp.property && tmp.op && isValueExist(tmp.value))
                        onChange?.(newItems)
                        setEditingItem({ index: -1 })
                    }}
                />
            )
        })
        tmps.push(
            <FilterRenderer
                // key={count.current}
                value={{}}
                isEditing={isEditing}
                isDisabled={false}
                isFocus={editingItem?.index === -1}
                fields={fields}
                style={{ flex: 1 }}
                onClick={() => {
                    if (editingItem?.index !== -1) setEditingItem({ index: -1 })
                }}
                // @ts-ignore
                containerRef={ref}
                onRemove={() => {
                    const index = items.length - 1 < 0 ? 0 : items.length - 1
                    console.log('index: ', index)
                    setEditingItem({ index })
                }}
                onChange={(newValue: any) => {
                    let newItems = [...items]
                    newItems.push(newValue)
                    newItems = newItems.filter((tmp) => tmp && tmp.property && tmp.op && isValueExist(tmp.value))
                    onChange?.(newItems)
                }}
            />
        )
        return tmps
    }, [items, isEditing, editingItem, onChange, fields])

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
            <div className={styles.placeholder}>
                {!isEditing && items.length === 0 && (
                    <LabelSmall $style={{ color: 'rgba(2,16,43,0.40)', position: 'absolute' }}>
                        {t('table.search.placeholder')}
                    </LabelSmall>
                )}
            </div>
            <div className={styles.filters}>{filters}</div>
        </div>
    )
}

export function DatastoreMixedTypeSearch({
    fields,
    ...props
}: Omit<ISearchProps, 'fields'> & { fields: RecordSchemaT[] }) {
    const searchColumns = useSearchColumns(fields)
    return <Search {...props} fields={searchColumns} />
}
