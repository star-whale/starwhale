import React, { useRef, useMemo } from 'react'
import { Skeleton } from 'baseui/skeleton'
import { areEqual } from 'react-window'
import { useStyletron } from 'baseui'
import { createUseStyles } from 'react-jss'
import cn from 'classnames'
import { IContextGridTable, ITableProps } from './types'
import Pagination from './Pagination'
import { BusyPlaceholder } from '../BusyLoaderWrapper'
import { StatefulDataTable } from '../base/data-table'
import { stateSelector } from '../base/data-table/store'
import { useStore, useStoreApi } from './hooks/useStore'
import { StoreProvider } from './store/StoreProvider'
import StoreUpdater from './store/StoreUpdater'

const useStyles = createUseStyles({
    table: {
        'width': '100%',
        'height': '100%',
        'position': 'relative',
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
    compareable = false,
    selectable = false,
    queryinline = false,
    onSave,
    onChange = () => {},
    emptyMessage,
    emptyColumnMessage,
    getId = (record: any) => record.id,
    storeRef,
    onColumnsChange,
    children,
}: ITableProps) {
    const wrapperRef = useRef<HTMLDivElement>(null)
    const [, theme] = useStyletron()
    const styles = useStyles({ theme })
    const store = useStoreApi()
    const $rows = useMemo(
        () =>
            data.map((raw, index) => {
                return {
                    id: getId(raw) ?? index.toFixed(),
                    data: raw,
                }
            }),
        [data]
    )

    // const $filters = useMemo(() => {
    //     return store.currentView?.filters
    // }, [store])

    // React.useEffect(() => {
    //     const unsub = store.subscribe(stateSelector, onChange)
    //     return unsub
    // }, [store, onChange])

    // React.useEffect(() => {
    //     if (!storeRef) return
    //     // eslint-disable-next-line no-param-reassign
    //     storeRef.current = store
    // }, [storeRef, store])

    return (
        <>
            <div
                className={cn(styles.table, styles.tablePinnable, compareable ? styles.tableCompareable : undefined)}
                ref={wrapperRef}
            >
                {children}
                <StatefulDataTable
                    resizableColumnWidths
                    searchable={searchable}
                    queryinline={queryinline}
                    filterable={filterable}
                    queryable={queryable}
                    compareable={compareable}
                    selectable={selectable}
                    loading={!!isLoading}
                    rowActions={rowActions}
                    columns={columns}
                    rows={$rows}
                    onSave={onSave}
                    getId={getId}
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
            <StoreUpdater {...rest} />
        </StoreProvider>
    )
}
