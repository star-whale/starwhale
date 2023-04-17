import React, { useCallback, useMemo, useRef, useState } from 'react'
import { Search } from 'baseui/icon'
import { SIZE as INPUT_SIZES } from 'baseui/input'
import Input from '../../Input'
import { useStyletron } from 'baseui'
import _ from 'lodash'
import { DataTable } from './data-custom-table'
import { StatefulContainer } from './stateful-container'
import { LocaleContext } from 'baseui/locale'
import type { ColumnT, ConfigT, StatefulContainerPropsT, StatefulDataTablePropsT } from './types'
import ConfigManageColumns from './config-manage-columns'
import FilterOperateMenu from './filter-operate-menu'
import ConfigViews from './config-views'
import { Operators } from './filter-operate-selector'
import { useResizeObserver } from '../../utils/useResizeObserver'
import { useConfigQuery } from './config-query'
import Button from '../../Button'
import classNames from 'classnames'
import { themedUseStyletron } from '../../theme/styletron'
import useCurrentView from './view/useCurrentView'
import { BusyPlaceholder } from '@starwhale/ui'

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
    const [css] = themedUseStyletron()
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
    const queryinline = props.queryinline === undefined ? true : props.queryinline

    const { useStore } = props
    const store = useStore()
    const { columns: $columns, currentView, isAllRuns } = useCurrentView(useStore, { columns: props.columns })
    const { renderConfigQuery } = useConfigQuery(useStore, { columns: props.columns, queryable })

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
    const handleSave = async (view: ConfigT) => {
        if (!view.id || view.id === 'all') store.onShowViewModel(true, view)
        else {
            store.onViewUpdate(view)
            await props.onSave?.(view)
        }
    }
    const handleSaveAs = useCallback(
        (view) => {
            store.onShowViewModel(true, {
                ...view,
                id: undefined,
                updated: false,
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

    // changed status must be after all the store changes(after api success)
    const changed = store.currentView.updated

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
                    <div
                        data-type='table-toolbar'
                        className={css({
                            height: `${headlineHeight}px`,
                            display: viewable || filterable || searchable || queryable || columnable ? 'block' : 'none',
                        })}
                    >
                        <div ref={headlineRef} className='flex-row-left g-20' style={{ paddingBottom: '20px' }}>
                            <div
                                className={classNames(
                                    'g-20 ',
                                    css({
                                        'display': 'grid',
                                        'gridTemplateColumns': 'minmax(200px, 280px) auto auto',
                                        ':first-child': {
                                            marginBottom: '20px',
                                        },
                                    })
                                )}
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

                                {viewable && changed && !$rowSelectedIds.size && (
                                    <div>
                                        {!isAllRuns && (
                                            <>
                                                <Button onClick={() => handleSave(store.currentView)}>Save</Button>
                                                &nbsp;&nbsp;
                                            </>
                                        )}

                                        <Button onClick={() => handleSaveAs(store.currentView)}>Save As</Button>
                                    </div>
                                )}
                            </div>
                            <div
                                className={classNames(
                                    css({
                                        gridTemplateColumns: 'minmax(200px,1fr) auto',
                                        display: 'grid',
                                        marginBottom: queryable && columnable ? '20px' : '0px',
                                    })
                                )}
                            >
                                <div className='table-config-query' style={{ flex: 1 }}>
                                    {renderConfigQuery()}
                                </div>

                                {columnable && !$rowSelectedIds.size && (
                                    <div className='table-config-column flex-row-center'>
                                        <ConfigManageColumns
                                            view={currentView}
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
                        <DataTable
                            useStore={props.useStore}
                            columns={$columns}
                            selectable={selectable}
                            compareable={compareable}
                            queryinline={queryinline}
                            rawColumns={props.columns}
                            emptyMessage={props.emptyMessage}
                            getId={props.getId}
                            filters={$filtersEnabled}
                            loading={props.loading}
                            loadingMessage={props.loadingMessage}
                            onIncludedRowsChange={onIncludedRowsChange}
                            onRowHighlightChange={onRowHighlightChange}
                            onSelectMany={onSelectMany}
                            onSelectNone={onSelectNone}
                            onSelectOne={onSelectOne}
                            resizableColumnWidths={resizableColumnWidths}
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
                        {$columns.length === 0 && props.emptyColumnMessage}
                    </div>
                </>
            )}
        </StatefulContainer>
    )
}
