import { ColumnFilterModel, ColumnSchemaDesc, TableQueryFilterDesc } from '@starwhale/core/datastore'
import { createUseStyles } from 'react-jss'
import React, { useState, useRef, useEffect } from 'react'
import FilterRenderer from './FilterRenderer'
import { ValueT } from './types'
import { useClickAway } from 'react-use'
import qs from 'qs'
import { useQueryArgs } from '../../../../src/hooks/useQueryArgs'
import { useDeepEffect } from '../../../../src/hooks/useDeepEffects'

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
        maxWidth: '100px',
        overflow: ' hidden',
        display: 'flex',
        alignItems: 'center',
    },
})

export interface ISearchProps {
    fields: ColumnSchemaDesc[]
}

export default function Search({ ...props }: ISearchProps) {
    const styles = useStyles()
    const raw = [{}]
    const ref = useRef<HTMLDivElement>(null)
    const { query, updateQuery } = useQueryArgs()

    const [isEditing, setIsEditing] = useState(false)

    const [items, setItems] = useState<ValueT[]>(query.filter ? query.filter.filter((v: any) => v.value) : (raw as any))

    useClickAway(ref, () => setIsEditing(false))

    const column = React.useMemo(() => new ColumnFilterModel(props.fields), [props.fields])

    useEffect(() => {
        if (!query.filter) setItems(raw as any)
    }, [query.filter])

    useDeepEffect(() => {
        updateQuery({ filter: items.filter((v) => v.value) as any })
    }, [items, column])

    return (
        <div
            className={styles.searchBar}
            ref={ref}
            style={{ borderColor: isEditing ? '#799EE8' : '#CFD7E6' }}
            onFocus={(e) => {
                if (e.target.classList.contains('filter-remove')) return
                setIsEditing(true)
            }}
        >
            {items.map((item, index) => {
                return (
                    <FilterRenderer
                        key={[index, item.property].join('-')}
                        value={item}
                        isEditing={true}
                        isDisabled={false}
                        isFocus={index === items.length - 1 && isEditing}
                        column={column}
                        style={{
                            flex: index === items.length - 1 ? 1 : undefined,
                        }}
                        containerRef={ref}
                        onChange={(value) => {
                            let newItems = []
                            if (!value) {
                                newItems = items.filter((_, i) => i !== index)
                            } else {
                                newItems = items.map((item, i) => (i === index ? value : item))
                            }
                            if (newItems.length === 0) {
                                newItems = [{}]
                            }
                            setItems(newItems)

                            if (value && value.property && value.op && value.value) {
                                setItems([...newItems, {}])
                                setIsEditing(true)
                            }
                        }}
                    />
                )
            })}
        </div>
    )
}
