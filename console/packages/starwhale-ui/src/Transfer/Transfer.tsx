import { useStyletron } from 'baseui'
import { Search } from 'baseui/icon'
import classNames from 'classnames'
import React, { useCallback, useEffect, useMemo } from 'react'
import { createUseStyles } from 'react-jss'
import { ColumnT, ConfigT } from '../base/data-table/types'
import Button from '../Button'
import IconFont from '../IconFont'
import Input from '../Input'
import TransferList from './TransferList'
import { useDeepEffect } from '@starwhale/core/utils'

const useStyles = createUseStyles({
    transfer: {
        'flex': 1,
        'display': 'flex',
        'flexDirection': 'column',
        'gap': '20px',
        'overflow': 'hidden',
        '& .query': {
            width: '280px',
        },
        '& .list': {
            display: 'flex',
            flex: 1,
            overflow: 'hidden',
        },
        '& .transfer-list': {
            flex: 1,
            borderRadius: '4px',
            display: 'flex',
            overflow: 'hidden',
        },
        '& .transfer-list-content': {
            border: '1px solid #CFD7E6',
            flex: '1',
            display: 'flex',
            flexDirection: 'column',
        },
        '& .transfer-list-toolbar': {
            display: 'flex',
            flex: 'none',
            flexDirection: 'column',
            alignSelf: 'center',
            margin: '0 10px',
            verticalAlign: 'middle',
            gap: '20px',
        },
        '& .transfer-list-content-header': {
            display: 'flex',
            height: '42px',
            borderBottom: '1px solid #EEF1F6',
            marginBottom: '8px',
            fontSize: '14px',
            marginLeft: '10px',
            marginRight: '9px',
            alignItems: 'center',
            gap: '9px',
            flex: 'none',
        },
        '& .transfer-list-content-body': {
            display: 'flex',
            flexDirection: 'column',
            overflow: 'hidden',
        },
        '& .transfer-list-content-ul': {
            overflow: 'auto',
        },
        '& .transfer-list-content-item': {
            'paddingLeft': '10px',
            'paddingRight': '9px',
            'display': 'flex',
            'alignItems': 'center',
            'gap': '9px',
            'height': '32px',
            'willChange': 'transform',
            'flexWrap': 'nowrap',
            'justifyContent': 'space-between',
            'background': '#FFFFFF',
            '&:hover': {
                background: '#F0F4FF',
            },
        },
    },
})

type TransferValueT = {
    selectedIds: any[]
    pinnedIds: any[]
    sortedIds: any[]
}

type TransferPropsT = {
    isDragable?: boolean
    isSearchable?: boolean
    value?: TransferValueT
    columns?: ColumnT[]
    onChange?: (args: TransferValueT) => void
}
const defaultValue = { selectedIds: [], pinnedIds: [], sortedIds: [] }

export function matchesQuery(text: string, query: string): boolean {
    return text.toLowerCase().includes(query.toLowerCase())
}

export default function Transfer({
    isDragable = false,
    isSearchable = false,
    columns = [],
    value = defaultValue,
    onChange = () => {},
}: TransferPropsT) {
    const styles = useStyles()
    const [left, setLeft] = React.useState<TransferValueT>(defaultValue)
    const [right, setRight] = React.useState<TransferValueT>(defaultValue)
    const [css, theme] = useStyletron()
    const [query, setQuery] = React.useState('')

    const columnAllIds = useMemo(() => {
        return columns.map((v) => v.key as string)
    }, [columns])
    const matchedColumns = React.useMemo(() => {
        return columns.filter((column) => matchesQuery(column.title, query)) ?? []
    }, [columns, query])
    const columnMatchedIds = useMemo(() => {
        return matchedColumns.map((v) => v.key) ?? []
    }, [matchedColumns])

    // keep raw list
    const $left = useMemo(() => {
        return columnAllIds
            .filter((id) => !value.selectedIds?.includes(id))
            .filter((id) => columnMatchedIds.includes(id))
    }, [columnMatchedIds, value.selectedIds])

    const $right = useMemo(() => {
        return columnAllIds
            .filter((id) => value.selectedIds?.includes(id))
            .filter((id) => columnMatchedIds.includes(id))
    }, [columnMatchedIds, value.selectedIds])

    // move action
    const handleToRight = useCallback(() => {
        onChange({ ...right, selectedIds: [...value.selectedIds, ...left.selectedIds] })
    }, [left, right, value])

    const handleToLeft = useCallback(() => {
        onChange({ ...right, selectedIds: value.selectedIds?.filter((id: any) => !right.selectedIds.includes(id)) })
    }, [left, right, value])

    // waiting for value changed action
    useEffect(() => {
        // , selectedIds: []
        setLeft({ ...left })
        setRight({ ...value })
    }, [value.selectedIds])

    return (
        <div className={styles.transfer}>
            {isSearchable && (
                <div className='query'>
                    <Input
                        overrides={{
                            Before: function Before() {
                                return (
                                    <div
                                        className={css({
                                            alignItems: 'center',
                                            display: 'flex',
                                            paddingLeft: theme.sizing.scale500,
                                        })}
                                    >
                                        <Search size='18px' />
                                    </div>
                                )
                            },
                        }}
                        value={query}
                        // @ts-ignore
                        onChange={(event) => setQuery(event.target.value)}
                    />
                </div>
            )}
            <div className='list'>
                <TransferList value={left} onChange={setLeft as any} columns={columns} raw={$left} />
                <div className='transfer-list-toolbar'>
                    <Button disabled={left.selectedIds.length === 0} onClick={handleToRight}>
                        <IconFont type='arrow_right' />
                    </Button>
                    <Button disabled={right.selectedIds.length === 0} onClick={handleToLeft}>
                        <IconFont type='arrow_left' />
                    </Button>
                </div>
                <TransferList
                    value={right}
                    onChange={setRight as any}
                    columns={columns}
                    isDragable={isDragable}
                    raw={$right}
                />
            </div>
        </div>
    )
}
