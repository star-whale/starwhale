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
import { useStoreApi } from '@starwhale/ui/GridTable/hooks/useStore'

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
    const headlineRef = React.useRef(null)
    const [headlineHeight, setHeadlineHeight] = React.useState(64)
    useResizeObserver(headlineRef, (entries) => {
        setHeadlineHeight(entries[0].contentRect.height)
    })
    const { filterable, searchable, columnable, viewable, compareable, queryable, selectable, queryinline } = props
    const { columns: $columns, currentView, isAllRuns } = useCurrentView({ columns: props.columns })
    const { renderConfigQuery } = useConfigQuery({ columns: props.columns, queryable })

    const store = useStoreApi().getState()

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
                    {/* <div
                        data-type='table-toolbar'
                        className={css({
                            height: `${headlineHeight}px`,
                            display: viewable || filterable || searchable || queryable || columnable ? 'block' : 'none',
                        })}
                    >
                        <div ref={headlineRef} className='flex-row-left g-20' style={{ paddingBottom: '20px' }}></div>
                    </div> */}

                    <div
                        data-type='table-wrapper'
                        style={{ width: '100%', height: `calc(100% - ${headlineHeight}px)` }}
                    >
                        {/* @ts-ignore */}
                        <DataTable
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
