/*
Copyright (c) Uber Technologies, Inc.

This source code is licensed under the MIT license found in the
LICENSE file in the root directory of this source tree.
*/

import React from 'react'
import ResizeObserver from 'resize-observer-polyfill'

import { Button, SHAPE as BUTTON_SHAPES, SIZE as BUTTON_SIZES, KIND as BUTTON_KINDS } from 'baseui/button'
import { Search } from 'baseui/icon'
import { Input, SIZE as INPUT_SIZES } from 'baseui/input'
import { Popover } from 'baseui/popover'
import { useStyletron } from 'baseui'
import { Tag } from 'baseui/tag'
import FilterMenu from './filter-menu'
import { DataTable } from './data-custom-table'
import { StatefulContainer } from './stateful-container'
import { LocaleContext } from './locales'
import type { ColumnT, RowT, StatefulContainerPropsT, StatefulDataTablePropsT } from './types'
import ConfigManageColumns from './config-manage-columns'

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
        <div className={css({ width: '375px', marginBottom: theme.sizing.scale500 })}>
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
        </div>
    )
}

interface IFilterTags {
    title: string
    filter: { description: string }
    onFilterAdd: (v: string, { description: string }: any) => any
    onFilterRemove: (v: string) => any
    columns: ColumnT[]
    rows: RowT[]
}
function FilterTag(props: IFilterTags) {
    const [, theme] = useStyletron()
    const [isOpen, setIsOpen] = React.useState(false)
    // @ts-ignore
    const columnIndex = props.columns.findIndex((c) => c.title === props.title)
    const column = props.columns[columnIndex]
    if (!column) {
        return null
    }

    const data = props.rows.map((r) => column.mapDataToValue(r.data))
    const Filter = column.renderFilter

    return (
        <Popover
            focusLock
            returnFocus
            key={props.title}
            isOpen={isOpen}
            onClickOutside={() => setIsOpen(false)}
            content={() => (
                // @ts-ignore
                <Filter
                    close={() => setIsOpen(false)}
                    data={data}
                    filterParams={props.filter}
                    setFilter={(filterParams: any) => props.onFilterAdd(props.title, filterParams)}
                />
            )}
        >
            <div>
                <Tag
                    onClick={() => setIsOpen(!isOpen)}
                    onActionClick={() => props.onFilterRemove(props.title)}
                    overrides={{
                        Root: {
                            style: {
                                borderTopLeftRadius: '36px',
                                borderTopRightRadius: '36px',
                                borderBottomLeftRadius: '36px',
                                borderBottomRightRadius: '36px',
                                height: '36px',
                                marginTop: 0,
                                marginBottom: theme.sizing.scale500,
                            },
                        },
                        Action: {
                            style: {
                                borderTopRightRadius: '36px',
                                borderBottomRightRadius: '36px',
                                height: '22px',
                            },
                        },
                        Text: {
                            style: {
                                maxWidth: '160px',
                            },
                        },
                    }}
                >
                    {props.title}: {props.filter.description}
                </Tag>
            </div>
        </Popover>
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

    return (
        <StatefulContainer
            batchActions={props.batchActions}
            columns={props.columns}
            initialFilters={props.initialFilters}
            initialSelectedRowIds={props.initialSelectedRowIds}
            initialSortIndex={props.initialSortIndex}
            initialSortDirection={props.initialSortDirection}
            onFilterAdd={props.onFilterAdd}
            onFilterRemove={props.onFilterRemove}
            onIncludedRowsChange={props.onIncludedRowsChange}
            onRowHighlightChange={props.onRowHighlightChange}
            onSelectionChange={props.onSelectionChange}
            resizableColumnWidths={props.resizableColumnWidths}
            rows={props.rows}
            rowActions={props.rowActions}
            rowHighlightIndex={props.rowHighlightIndex}
            onColumnSave={props.onColumnSave}
            config={props.config}
        >
            {/* @ts-ignore */}
            {({
                filters,
                onFilterAdd,
                onFilterRemove,
                onIncludedRowsChange,
                onRowHighlightChange,
                onSelectMany,
                onSelectNone,
                onSelectOne,
                onSort,
                onTextQueryChange,
                resizableColumnWidths,
                rowHighlightIndex,
                selectedRowIds,
                sortIndex,
                sortDirection,
                textQuery,
                onColumnSave,
                config,
            }: StatefulContainerPropsT['children']) => (
                <>
                    <div className={css({ height: `${headlineHeight}px` })}>
                        <div
                            ref={headlineRef}
                            className={css({
                                display: 'flex',
                                justifyContent: 'space-between',
                            })}
                        >
                            {!selectedRowIds.size && (
                                <div
                                    className={css({
                                        alignItems: 'end',
                                        display: 'flex',
                                        flexWrap: 'wrap',
                                        paddingTop: theme.sizing.scale500,
                                    })}
                                >
                                    {searchable && <QueryInput onChange={onTextQueryChange} />}

                                    {filterable && (
                                        <>
                                            <FilterMenu
                                                columns={props.columns}
                                                filters={filters}
                                                rows={props.rows}
                                                onSetFilter={onFilterAdd}
                                            />

                                            {Array.from(filters).map(([title, filter]) => (
                                                <FilterTag
                                                    key={title}
                                                    columns={props.columns}
                                                    filter={filter}
                                                    onFilterAdd={onFilterAdd}
                                                    onFilterRemove={onFilterRemove}
                                                    rows={props.rows}
                                                    title={title}
                                                />
                                            ))}
                                        </>
                                    )}
                                </div>
                            )}

                            {Boolean(selectedRowIds.size) && props.batchActions && (
                                <div
                                    style={{
                                        display: 'flex',
                                        alignItems: 'center',
                                        paddingTop: theme.sizing.scale400,
                                        paddingBottom: theme.sizing.scale400,
                                    }}
                                >
                                    {props.batchActions.map((action) => {
                                        const onClick = (event: React.SyntheticEvent<HTMLButtonElement>) => {
                                            action.onClick({
                                                clearSelection: onSelectNone,
                                                event,
                                                selection: props.rows.filter((r) => selectedRowIds.has(r.id)),
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

                            <div
                                className={css({
                                    alignItems: 'center',
                                    display: 'flex',
                                    flexWrap: 'wrap',
                                    paddingTop: theme.sizing.scale500,
                                })}
                            >
                                <ConfigManageColumns
                                    columns={props.columns}
                                    onColumnSave={onColumnSave}
                                    config={config}
                                    // onSaveAs={onColumnSaveAs}
                                />
                            </div>
                        </div>
                    </div>

                    <div style={{ width: '100%', height: `calc(100% - ${headlineHeight}px)` }}>
                        {/* @ts-ignore */}
                        <DataTable
                            batchActions={props.batchActions}
                            columns={props.columns}
                            emptyMessage={props.emptyMessage}
                            filters={filters}
                            loading={props.loading}
                            loadingMessage={props.loadingMessage}
                            onIncludedRowsChange={onIncludedRowsChange}
                            onRowHighlightChange={onRowHighlightChange}
                            onSelectionChange={props.onSelectionChange}
                            onSelectMany={onSelectMany}
                            onSelectNone={onSelectNone}
                            onSelectOne={onSelectOne}
                            onSort={onSort}
                            resizableColumnWidths={resizableColumnWidths}
                            rowHighlightIndex={rowHighlightIndex}
                            rows={props.rows}
                            rowActions={props.rowActions}
                            rowHeight={props.rowHeight}
                            selectedRowIds={selectedRowIds}
                            sortDirection={sortDirection}
                            sortIndex={sortIndex}
                            textQuery={textQuery}
                            controlRef={props.controlRef}
                        />
                    </div>
                </>
            )}
        </StatefulContainer>
    )
}
