import React, { useCallback, useMemo } from 'react'
import ResizeObserver from 'resize-observer-polyfill'

import { Button, SHAPE as BUTTON_SHAPES, SIZE as BUTTON_SIZES, KIND as BUTTON_KINDS } from 'baseui/button'
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

// @ts-ignore
function useResizeObserver(
    ref: { current: HTMLElement | null },
    callback: (entires: ResizeObserverEntry[], obs: ResizeObserver) => any
) {
    React.useLayoutEffect(() => {
        if (ref.current) {
            const observer = new ResizeObserver(callback)
            observer.observe(ref.current)
            return () => observer.disconnect()
        }
        // @eslint-disable-next-line consistent-return
        return () => {}
    }, [ref, callback])
}

function QueryInput(props: any) {
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
    const { useStore } = props
    const store = useStore()

    const { columns } = props
    const { pinnedIds = [], selectedIds = [] }: ConfigT = store.currentView || {}

    const $columns = useMemo(() => {
        if (!columnable) return columns

        const columnsMap = _.keyBy(columns, (c) => c.key) as Record<string, ColumnT>

        return selectedIds
            .filter((id: any) => id in columnsMap)
            .map((id: any) => {
                return {
                    ...columnsMap[id],
                    pin: pinnedIds.includes(id) ? 'LEFT' : undefined,
                }
            }) as ColumnT[]
    }, [columns, columnable, pinnedIds, selectedIds])

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
        (selectedIds, pinnedIds, sortedIds) => {
            store.onCurrentViewColumnsChange(selectedIds, pinnedIds, sortedIds)
        },
        [store]
    )
    const handleSave = useCallback(
        (view) => {
            store.onShowViewModel(true, view)
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

    const handleFilterSave = useCallback(
        (filters) => {
            store.onShowViewModel(true, {
                ...store.currentView,
                filters,
            })
        },
        [store]
    )

    const handleFilterSaveAs = useCallback(
        (filters) => {
            store.onShowViewModel(true, {
                ...store.currentView,
                id: undefined,
                filters,
            })
        },
        [store]
    )

    const { rowSelectedIds, onSelectMany, onSelectNone, onSelectOne } = store
    const $rowSelectedIds = useMemo(() => new Set(Array.from(rowSelectedIds)), [rowSelectedIds])
    const [$sortIndex, $sortDirection] = useMemo(() => {
        const { sortBy, sortDirection } = store.currentView || {}
        const sortIndex = $columns.findIndex((c) => c.key === sortBy)
        return [sortIndex, sortDirection]
    }, [store, $columns])

    return (
        <StatefulContainer
            batchActions={props.batchActions}
            // @ts-ignore
            columns={$columns}
            initialSortIndex={props.initialSortIndex}
            initialSortDirection={props.initialSortDirection}
            onIncludedRowsChange={props.onIncludedRowsChange}
            onRowHighlightChange={props.onRowHighlightChange}
            onSelectionChange={props.onSelectionChange}
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
                        <div
                            ref={headlineRef}
                            className={css({
                                display: 'flex',
                                justifyContent: 'space-between',
                            })}
                        >
                            <div
                                className='flex-row-center mb-20 g-20'
                                style={{
                                    gridTemplateColumns: '1fr auto 1fr',
                                    display: 'grid',
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
                                        filters={store.currentView.filters ?? []}
                                        columns={props.columns}
                                        rows={props.rows}
                                        onFilterSet={handeFilterSet}
                                        onSave={handleFilterSave}
                                        onSaveAs={handleFilterSaveAs}
                                    />
                                )}

                                {searchable && <QueryInput onChange={onTextQueryChange} />}
                            </div>

                            {false && Boolean($rowSelectedIds.size) && props.batchActions && (
                                <div
                                    style={{
                                        display: 'flex',
                                        alignItems: 'center',
                                        paddingTop: theme.sizing.scale400,
                                        paddingBottom: theme.sizing.scale400,
                                    }}
                                >
                                    {props.batchActions?.map((action) => {
                                        const onClick = (event: React.SyntheticEvent<HTMLButtonElement>) => {
                                            action.onClick({
                                                clearSelection: onSelectNone,
                                                event,
                                                selection: props.rows.filter((r) => $rowSelectedIds.has(r.id)),
                                            })
                                        }

                                        if (action.renderIcon) {
                                            const Icon = action.renderIcon
                                            return (
                                                <Button
                                                    key={action.label}
                                                    overrides={{
                                                        BaseButton: { props: { 'aria-label': action.label } },
                                                    }}
                                                    onClick={onClick}
                                                    kind={BUTTON_KINDS.tertiary}
                                                    shape={BUTTON_SHAPES.round}
                                                >
                                                    {/* @ts-ignore */}
                                                    <Icon size={16} />
                                                </Button>
                                            )
                                        }

                                        return (
                                            <Button
                                                key={action.label}
                                                onClick={onClick}
                                                kind={BUTTON_KINDS.secondary}
                                                size={BUTTON_SIZES.compact}
                                            >
                                                {action.label}
                                            </Button>
                                        )
                                    })}
                                </div>
                            )}

                            {columnable && !$rowSelectedIds.size && (
                                <div className='flex-row-center mb-20'>
                                    <ConfigManageColumns
                                        view={store.currentView}
                                        columns={props.columns}
                                        onApply={handleApply}
                                        onSave={handleSave}
                                        onSaveAs={handleSaveAs}
                                    />
                                </div>
                            )}
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
                                batchActions={props.batchActions}
                                columns={$columns}
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
