import React, { useCallback, useMemo, useRef, useState } from 'react'
import { Search } from 'baseui/icon'
import { SIZE as INPUT_SIZES } from 'baseui/input'
import Input from '@/components/Input'
import { useStyletron } from 'baseui'
import _ from 'lodash'
import { DataTable } from './data-custom-table'
import { StatefulContainer } from './stateful-container'
import { LocaleContext } from './locales'
import type { ColumnT, ConfigT, StatefulContainerPropsT, StatefulDataTablePropsT } from './types'
import ConfigManageColumns from './config-manage-columns'
import FilterOperateMenu from './filter-operate-menu'
import ConfigViews from './config-views'
import { Operators } from './filter-operate-selector'
import { useResizeObserver } from '../../utils/useResizeObserver'
import ConfigQuery from './config-query'
import { ITableState } from './store'
import Button from '@starwhale/ui/Button'

export function QueryInput(props: any) {
    const [css, theme] = useStyletron()
    const locale = React.useContext(LocaleContext)
    const [value, setValue] = React.useState('')
    const { onChange } = props

    React.useEffect(() => {
        const timeout = setTimeout(() => onChange(value), 250)
        return () => clearTimeout(timeout)
    }, [onChange, value])

    return (
        <Input
            aria-label={locale.datatable.searchAriaLabel}
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
            size={INPUT_SIZES.compact}
            onChange={(event) => setValue((event.target as HTMLInputElement).value)}
            value={value}
            clearable
        />
    )
}

export function StatefulDataTable(props: StatefulDataTablePropsT) {
    const [css, theme] = useStyletron()
    const headlineRef = React.useRef(null)
    const [headlineHeight, setHeadlineHeight] = React.useState(64)
    useResizeObserver(headlineRef, (entries) => {
        setHeadlineHeight(entries[0].contentRect.height)
    })

    const filterable = props.filterable === undefined ? true : props.filterable
    const searchable = props.searchable === undefined ? true : props.searchable
    const columnable = props.columnable === undefined ? true : props.columnable
    const viewable = props.viewable === undefined ? true : props.viewable
    const compareable = props.viewable === undefined ? true : props.compareable
    const queryable = props.viewable === undefined ? true : props.queryable
    const selectable = props.selectable === undefined ? true : props.selectable

    const { useStore } = props
    const store = useStore()

    const { columns } = props
    const { pinnedIds = [], ids = [] }: ConfigT = store.currentView || {}

    const $columns = useMemo(() => {
        // if (!columnable) return columns

        const columnsMap = _.keyBy(columns, (c) => c.key) as Record<string, ColumnT>

        return ids
            .filter((id: any) => id in columnsMap)
            .map((id: any) => {
                return {
                    ...columnsMap[id],
                    pin: pinnedIds.includes(id) ? 'LEFT' : undefined,
                }
            }) as ColumnT[]
    }, [columns, columnable, pinnedIds, ids])

    const $filters = React.useMemo(() => {
        return (
            props.initialFilters?.map((v) => ({
                ...v,
                op: Operators[v.op?.key || 'default'],
            })) || []
        )
    }, [props.initialFilters])

    const $filtersEnabled = React.useMemo(() => {
        return $filters?.filter((c) => !c.disable)
    }, [$filters])

    const handleApply = useCallback(
        // eslint-disable-next-line @typescript-eslint/no-shadow
        (selectedIds, pinnedIds, ids) => {
            store.onCurrentViewColumnsChange(selectedIds, pinnedIds, ids)
        },
        [store]
    )
    const handleSave = useCallback(
        (view) => {
            if (!view.id || view.id === 'id') store.onShowViewModel(true, view)
            else {
                props.onSave?.(view).then()
            }
        },
        [store]
    )
    const handleSaveAs = useCallback(
        (view) => {
            store.onShowViewModel(true, {
                ...view,
                id: undefined,
            })
        },
        [store]
    )

    const handeFilterSet = useCallback(
        (categories) => {
            store.onCurrentViewFiltersChange(categories)
        },
        [store]
    )

    const handeQuerySet = useCallback(
        (items) => {
            console.log('123')
            store.onCurrentViewQueriesChange(items)
        },
        [store]
    )

    // changed status must be after all the store changes(after api success)
    const [changed, setChanged] = useState(false)

    const prevUpdatedTime = useRef<number | undefined>(store.currentView.updatedTime)

    React.useEffect(() => {
        const unsub = useStore.subscribe(
            (state: ITableState) => state.currentView.updatedTime,
            (updatedTime?: number) => {
                if (!store.isInit) return

                console.log(prevUpdatedTime, updatedTime, props.loading)

                if (prevUpdatedTime.current !== updatedTime && !props.loading) {
                    setChanged(true)
                    prevUpdatedTime.current = updatedTime
                } else {
                    setChanged(false)
                }
            }
        )
        return unsub
    }, [store, props.loading, changed, prevUpdatedTime])

    const { rowSelectedIds, onSelectMany, onSelectNone, onSelectOne } = store
    const $rowSelectedIds = useMemo(() => new Set(Array.from(rowSelectedIds)), [rowSelectedIds])
    const [$sortIndex, $sortDirection] = useMemo(() => {
        const { sortBy, sortDirection } = store.currentView || {}
        const sortIndex = $columns.findIndex((c) => c.key === sortBy)
        return [sortIndex, sortDirection]
    }, [store, $columns])

    return (
        <StatefulContainer
            // @ts-ignore
            columns={$columns}
            initialSortIndex={props.initialSortIndex}
            initialSortDirection={props.initialSortDirection}
            onIncludedRowsChange={props.onIncludedRowsChange}
            onRowHighlightChange={props.onRowHighlightChange}
            resizableColumnWidths={props.resizableColumnWidths}
            rows={props.rows}
            rowActions={props.rowActions}
            rowHighlightIndex={props.rowHighlightIndex}
        >
            {/* @ts-ignore */}
            {({
                onIncludedRowsChange,
                onRowHighlightChange,
                onTextQueryChange,
                resizableColumnWidths,
                rowHighlightIndex,
                textQuery,
            }: StatefulContainerPropsT['children']) => (
                <>
                    <div data-type='table-toolbar' className={css({ height: `${headlineHeight}px` })}>
                        <div ref={headlineRef}>
                            <div
                                className='flex-row-left mb-20 g-20'
                                style={{
                                    display: 'grid',
                                    gridTemplateColumns: '280px auto auto',
                                }}
                            >
                                {viewable && (
                                    <ConfigViews
                                        useStore={useStore}
                                        store={store}
                                        columns={props.columns}
                                        rows={props.rows}
                                    />
                                )}
                                {filterable && (
                                    <FilterOperateMenu
                                        filters={store.currentView?.filters ?? []}
                                        columns={props.columns}
                                        rows={props.rows}
                                        onFilterSet={handeFilterSet}
                                    />
                                )}

                                {searchable && <QueryInput onChange={onTextQueryChange} />}

                                {changed && (
                                    <div>
                                        <Button onClick={() => handleSave(store.currentView)}>Save</Button>&nbsp;&nbsp;
                                        <Button onClick={() => handleSaveAs(store.currentView)}>Save As</Button>
                                    </div>
                                )}
                            </div>
                            <div
                                style={{
                                    gridTemplateColumns: '1fr auto',
                                    display: 'grid',
                                }}
                            >
                                {queryable && (
                                    <div className='table-config-query' style={{ flex: 1 }}>
                                        <ConfigQuery
                                            value={store.currentView?.queries ?? []}
                                            columns={props.columns}
                                            onChange={handeQuerySet}
                                        />
                                    </div>
                                )}

                                {columnable && !$rowSelectedIds.size && (
                                    <div className='table-config-column flex-row-center mb-20'>
                                        <ConfigManageColumns
                                            view={store.currentView}
                                            columns={props.columns}
                                            onApply={handleApply}
                                        />
                                    </div>
                                )}
                            </div>
                        </div>
                    </div>

                    <div
                        data-type='table-wrapper'
                        style={{ width: '100%', height: `calc(100% - ${headlineHeight}px)` }}
                    >
                        {/* @ts-ignore */}
                        {$columns.length > 0 && (
                            <DataTable
                                useStore={props.useStore}
                                columns={$columns}
                                selectable={selectable}
                                rawColumns={props.columns}
                                emptyMessage={props.emptyMessage}
                                filters={$filtersEnabled}
                                loading={props.loading}
                                loadingMessage={props.loadingMessage}
                                onIncludedRowsChange={onIncludedRowsChange}
                                onRowHighlightChange={onRowHighlightChange}
                                onSelectMany={onSelectMany}
                                onSelectNone={onSelectNone}
                                onSelectOne={onSelectOne}
                                resizableColumnWidths={resizableColumnWidths}
                                compareable={compareable}
                                rowHighlightIndex={rowHighlightIndex}
                                rows={props.rows}
                                rowActions={props.rowActions}
                                rowHeight={props.rowHeight}
                                selectedRowIds={$rowSelectedIds}
                                sortDirection={$sortDirection}
                                sortIndex={$sortIndex}
                                textQuery={textQuery}
                                controlRef={props.controlRef}
                            />
                        )}
                    </div>
                </>
            )}
        </StatefulContainer>
    )
}
