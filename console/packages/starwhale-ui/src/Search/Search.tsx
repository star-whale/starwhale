import { RecordSchemaT, isSearchColumns } from '@starwhale/core/datastore'
import { createUseStyles } from 'react-jss'
import React, { useRef } from 'react'
import { useClickAway } from 'react-use'
import FilterRenderer from './FilterRenderer'
import { SearchFieldSchemaT, ValueT } from './types'
import IconFont from '../IconFont'
import { LabelSmall } from 'baseui/typography'
import useTranslation from '@/hooks/useTranslation'
import { useDeepEffect } from '@starwhale/core'
import useSearchState from './SearchState'

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

export interface ISearchProps {
    fields: SearchFieldSchemaT[]
    value?: ValueT[]
    onChange?: (args: ValueT[]) => void
}

export default function Search({ value = [], onChange, fields }: ISearchProps) {
    // const trace = useTrace('grid-search')
    const styles = useStyles()
    const [t] = useTranslation()
    const ref = useRef<HTMLDivElement>(null)

    const state = useSearchState({ onChange })
    const {
        isEditing,
        editingIndex,
        focusToEnd,
        focusToPrevItem,
        onFocus,
        checkIsFocus,
        setIsEditing,
        onRemove,
        onRemoveThenBlur,
        onItemCreate,
        onItemChange,
        setItems,
    } = state

    useDeepEffect(() => {
        setItems(value)
    }, [value])

    const items = value

    useClickAway(ref, (e) => {
        if (containsNode(ref.current, e.target)) return
        if (containsNode(document.querySelector('.filter-popover'), e.target)) return
        setIsEditing(false)
    })

    const count = React.useRef(100)
    const filters = React.useMemo(() => {
        count.current += 1
        const tmps = items.map((item, index) => {
            return (
                <FilterRenderer
                    key={[index, item.property].join('-')}
                    value={item}
                    isFocus={checkIsFocus(index)}
                    fields={fields}
                    containerRef={ref}
                    focusToEnd={focusToEnd}
                    onClick={() => onFocus(index)}
                    onRemove={(blur) => (blur ? onRemoveThenBlur(index) : onRemove(index))}
                    onChange={(_value) => onItemChange(index, _value)}
                />
            )
        })
        tmps.push(
            <FilterRenderer
                value={{}}
                isFocus={checkIsFocus(-1)}
                fields={fields}
                style={{ flex: 1 }}
                onClick={focusToEnd}
                containerRef={ref}
                onRemove={focusToPrevItem}
                onChange={onItemCreate}
            />
        )
        return tmps
        // eslint-disable-next-line react-hooks/exhaustive-deps
    }, [isEditing, editingIndex, items, fields])

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
    fields: columnTypes,
    ...props
}: Omit<ISearchProps, 'fields'> & { fields: RecordSchemaT[] }) {
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
    return <Search {...props} fields={searchColumns} />
}
