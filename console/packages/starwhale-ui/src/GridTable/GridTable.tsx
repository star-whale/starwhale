import React, { useRef, useMemo } from 'react'
import { Skeleton } from 'baseui/skeleton'
import { areEqual } from 'react-window'
import { useStyletron } from 'baseui'
import { createUseStyles } from 'react-jss'
import cn from 'classnames'
import { IContextGridTable, ITableProps } from './types'
import { StoreProvider, useTableContext } from './StoreContext'
import Pagination from './Pagination'
import { BusyPlaceholder } from '../BusyLoaderWrapper'
import { StatefulDataTable } from '../base/data-table'
import { stateSelector } from '../base/data-table/store'

const useStyles = createUseStyles({
    table: {
        'width': '100%',
        'height': '100%',
        '& .baseui-table-cell-content': {},
        '& .column-cell .string-cell': {
            overflow: 'hidden',
            textOverflow: 'ellipsis',
            whiteSpace: 'nowrap',
        },
        '& .table-row': {
            '&:hover': {},
        },
        '& .table-columns-pinned': {
            backgroundColor: '#FFF',
        },
        '& .table-row--hovering': {
            backgroundColor: '#EBF1FF',
        },
        '& .table-row--hovering .table-cell': {
            backgroundColor: '#EBF1FF !important',
        },
        '& .table-row--hovering .column-cell > *': {
            backgroundColor: '#EBF1FF !important',
        },
    },
    tableCompareable: {
        '& .table-cell--last': {},
    },
    tablePinnable: {
        '& .table-columns-pinned': {
            borderRight: '1px solid rgb(207, 215, 230)',
        },
        '& .table-headers-pinned > div:last-child .header-cell': {
            borderRight: '1px solid rgb(207, 215, 230)',
        },
    },
})

function GridTable({
    isLoading,
    columns = [],
    data = [],
    paginationProps,
    rowActions,
    searchable = false,
    filterable = false,
    queryable = false,
    columnable = false,
    compareable = false,
    selectable = false,
    viewable = false,
    queryinline = false,
    onSave,
    onChange = () => {},
    emptyMessage,
    emptyColumnMessage,
    storeRef,
}: ITableProps) {
    const wrapperRef = useRef<HTMLDivElement>(null)
    const api = useTableContext()
    const store = api()
    const $rows = useMemo(
        () =>
            data.map((raw, index) => {
                return {
                    id: raw.id ?? index.toFixed(),
                    data: raw,
                }
            }),
        [data]
    )

    const $filters = useMemo(() => {
        return store.currentView?.filters
    }, [store])

    const [, theme] = useStyletron()
    const styles = useStyles({ theme })

    React.useEffect(() => {
        const unsub = api.subscribe(stateSelector, onChange)
        return unsub
    }, [api, onChange])

    React.useEffect(() => {
        if (!storeRef) return
        // eslint-disable-next-line no-param-reassign
        storeRef.current = store
    }, [storeRef, store])

    return (
        <>
            <div
                className={cn(styles.table, styles.tablePinnable, compareable ? styles.tableCompareable : undefined)}
                ref={wrapperRef}
            >
                <StatefulDataTable
                    store={store}
                    useStore={api}
                    resizableColumnWidths
                    initialFilters={$filters}
                    searchable={searchable}
                    queryinline={queryinline}
                    filterable={filterable}
                    queryable={queryable}
                    columnable={columnable}
                    compareable={compareable}
                    selectable={selectable}
                    viewable={viewable}
                    loading={!!isLoading}
                    rowActions={rowActions}
                    columns={columns}
                    rows={$rows}
                    onSave={onSave}
                    loadingMessage={() => (
                        <Skeleton
                            overrides={{
                                Root: {
                                    style: {
                                        paddingTop: '10px',
                                    },
                                },
                            }}
                            rows={10}
                            width='100%'
                            animation
                        />
                    )}
                    emptyMessage={emptyMessage ?? <BusyPlaceholder type='notfound' />}
                    emptyColumnMessage={emptyColumnMessage ?? <BusyPlaceholder type='notfound' />}
                />
            </div>
            <Pagination {...paginationProps} />
        </>
    )
}

export const MemoGridTable = React.memo(GridTable, areEqual)

export default function ContextGridTable({
    storeKey = 'table',
    initState = {},
    store = undefined,
    ...rest
}: IContextGridTable) {
    return (
        <StoreProvider initState={initState} storeKey={storeKey} store={store}>
            <MemoGridTable {...rest} />
        </StoreProvider>
    )
}
