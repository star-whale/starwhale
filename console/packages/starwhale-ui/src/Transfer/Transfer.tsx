import { useStyletron } from 'baseui'
import React, { useCallback, useMemo } from 'react'
import { createUseStyles } from 'react-jss'
import { ColumnT } from '../base/data-table/types'
import Button from '../Button'
import IconFont from '../IconFont'
import Input from '../Input'
import TransferList from './TransferList'
import useUnSortedSelection from '../utils/useUnsortedSelection'
import { BusyPlaceholder } from '../BusyLoaderWrapper'
import useTranslation from '@/hooks/useTranslation'

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
            display: 'flex',
            overflow: 'hidden',
            maxWidth: '400px',
        },
        '& .transfer-list-content': {
            borderRadius: '4px',
            border: '1px solid #CFD7E6',
            flex: '1',
            display: 'flex',
            flexDirection: 'column',
            minWidth: 0,
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
            flex: 1,
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
    ids: any[]
}

type TransferPropsT = {
    isDragable?: boolean
    isSearchable?: boolean
    value?: TransferValueT
    columns?: ColumnT[]
    onChange?: (args: TransferValueT) => void
}
const defaultValue = { selectedIds: [], pinnedIds: [], ids: [] }

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
    const [t] = useTranslation()
    const styles = useStyles()
    const [css, theme] = useStyletron()
    const [query, setQuery] = React.useState('')

    // eslint-disable-next-line no-param-reassign
    if (!value) value = defaultValue

    // cached & filtered columns
    const columnMap = useMemo(() => {
        const m = new Map()
        columns.forEach((v) => m.set(v.key, v))
        return m
    }, [columns])
    const columnAllIds = useMemo(() => {
        return columns.map((v) => v.key as string)
    }, [columns])
    const matchedColumns = React.useMemo(() => {
        return columns.filter((column) => matchesQuery(column.title, query)) ?? []
    }, [columns, query])
    const columnMatchedIds = useMemo(() => {
        return matchedColumns.map((v) => v.key) ?? []
    }, [matchedColumns])

    // computed: keep raw list
    const $leftFilteredColumns = useMemo(() => {
        return (
            columnAllIds
                .filter((id) => !value.ids?.includes(id))
                .filter((id) => columnMatchedIds.includes(id))
                .map((id) => columnMap.get(id)) ?? []
        )
    }, [columnMatchedIds, value.ids, columnMap, columnAllIds])

    const $rightFilteredColumns = useMemo(() => {
        return value.ids?.filter((id) => columnMatchedIds.includes(id)).map((id) => columnMap.get(id)) ?? []
    }, [columnMatchedIds, value.ids, columnMap])

    const leftOperators = useUnSortedSelection({
        initialIds: $leftFilteredColumns.map((v) => v.key),
        initialSelectedIds: [],
        initialPinnedIds: [],
    })
    const rightOperators = useUnSortedSelection({
        initialIds: $rightFilteredColumns.map((v) => v.key),
        initialSelectedIds: value?.selectedIds ?? [],
        initialPinnedIds: value?.pinnedIds ?? [],
    })

    // handelers: move action
    const handleToRight = useCallback(() => {
        onChange({
            selectedIds: rightOperators.selectedIds,
            pinnedIds: rightOperators.pinnedIds,
            ids: [...(value.ids ?? []), ...leftOperators.selectedIds],
        })
    }, [leftOperators, rightOperators, value, onChange])

    const handleToLeft = useCallback(() => {
        onChange({
            pinnedIds: rightOperators.pinnedIds,
            selectedIds: [],
            ids: value.ids?.filter((id: any) => !rightOperators.selectedIds.includes(id)),
        })
    }, [rightOperators, value, onChange])

    return (
        <div className={`${styles.transfer} inherit-height`}>
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
                                        <IconFont type='search' kind='gray' />
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
                <TransferList
                    columns={$leftFilteredColumns}
                    operators={leftOperators}
                    title={t('table.column.invisible')}
                />
                <div className='transfer-list-toolbar'>
                    <Button
                        disabled={leftOperators?.selectedIds?.length === 0}
                        onClick={handleToRight}
                        overrides={{
                            BaseButton: {
                                style: {
                                    height: '40px',
                                },
                            },
                        }}
                    >
                        <IconFont type='arrow_right' />
                    </Button>
                    <Button
                        disabled={rightOperators?.selectedIds?.length === 0}
                        onClick={handleToLeft}
                        overrides={{
                            BaseButton: {
                                style: {
                                    height: '40px',
                                },
                            },
                        }}
                    >
                        <IconFont type='arrow_left' />
                    </Button>
                </div>
                <TransferList
                    title={t('table.column.visible')}
                    emptyMessage={() => <BusyPlaceholder type='empty' style={{ minHeight: '0' }} />}
                    columns={$rightFilteredColumns}
                    isDragable={isDragable}
                    operators={{
                        ...rightOperators,
                        handlePinOne: (id: any) => {
                            const rtn = rightOperators.handlePinOne(id)
                            onChange({
                                selectedIds: rtn.selectedIds,
                                pinnedIds: rtn.pinnedIds,
                                ids: rtn.ids,
                            })
                        },
                        handleOrderChange: (ids: any[], dragId: any) => {
                            const rtn = rightOperators.handleOrderChange(ids, dragId)
                            onChange({
                                selectedIds: rtn.selectedIds,
                                pinnedIds: rtn.pinnedIds,
                                ids: rtn.ids,
                            })
                        },
                    }}
                />
            </div>
        </div>
    )
}
