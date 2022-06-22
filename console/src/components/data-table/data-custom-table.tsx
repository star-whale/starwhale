/*
Copyright (c) Uber Technologies, Inc.

This source code is licensed under the MIT license found in the
LICENSE file in the root directory of this source tree.
*/
// @flow

import React, { useCallback, useState } from 'react'
import { VariableSizeGrid, VariableSizeList } from 'react-window'
import AutoSizer from 'react-virtualized-auto-sizer'

import { Button, SHAPE as BUTTON_SHAPES, SIZE as BUTTON_SIZES, KIND as BUTTON_KINDS } from 'baseui/button'
import { useStyletron } from 'baseui'
import { Tooltip, PLACEMENT } from 'baseui/tooltip'
import { SORT_DIRECTIONS } from './constants'
import HeaderCell from './header-cell'
import MeasureColumnWidths from './measure-column-widths'
import type { ColumnT, DataTablePropsT, RowT, SortDirectionsT, RowActionT } from './types'
import { LocaleContext } from './locales'

// consider pulling this out to a prop if useful.
const HEADER_ROW_HEIGHT = 48

const IS_BROWSER = true

type HeaderContextT = {
    columns: ColumnT[]
    columnHighlightIndex: number
    emptyMessage: string | React.ComponentType<any>
    filters: DataTablePropsT['filters']
    loading: boolean
    loadingMessage: string | React.ComponentType<any>
    isScrollingX: boolean
    isSelectable: boolean
    isSelectedAll: boolean
    isSelectedIndeterminate: boolean
    measuredWidths: number[]
    onMouseEnter: (num: number) => void
    onMouseLeave: () => void
    onResize: (columnIndex: number, delta: number) => void
    onSelectMany: () => void
    onSelectNone: () => void
    onSort: (num: number) => void
    resizableColumnWidths: boolean
    rowActions: RowActionT[] | ((row: RowT) => RowActionT[])
    rowHeight: number
    rowHighlightIndex: number
    rows: RowT[]
    scrollLeft: number
    sortIndex: number
    sortDirection: SortDirectionsT
    tableHeight: number
    widths: number[]
}

type CellPlacementPropsT = {
    columnIndex: number
    rowIndex: number
    style: {
        position: string
        height: number
        width: number
        top: number
        left: number
    }
    data: {
        columns: ColumnT[]
        columnHighlightIndex: number
        isSelectable: boolean
        isRowSelected: (v: string | number) => boolean
        onRowMouseEnter: (num: number, row: RowT) => void
        onSelectOne: (row: RowT) => void
        rowHighlightIndex: number
        rows: RowT[]
        textQuery: string
        normalizedWidths: number[]
    }
}
// @ts-ignore
const sum = (ns) => ns.reduce((s, n) => s + n, 0)

function CellPlacement({ columnIndex, rowIndex, data, style }: any) {
    const [css, theme] = useStyletron()

    const [isHoverd, setIsHovered] = useState(false)

    const Cell = React.useMemo(() => {
        return data.columns[columnIndex].renderCell
    }, [columnIndex, data.columns])
    const value = data.columns[columnIndex].mapDataToValue(data.rows[rowIndex - 1].data)

    // ignores the table header row
    if (rowIndex === 0) {
        return null
    }

    return (
        <div
            className={
                // eslint-disable-next-line prefer-template
                css({
                    ...theme.borders.border200,
                    // backgroundColor,
                    borderTop: 'none',
                    borderBottom: 'none',
                    borderLeft: 'none',
                    // do not render a border on cells in the right-most column
                    // borderRight: columnIndex === data.columns.length - 1 ? 'none' : undefined,
                    border: 'none',
                    boxSizing: 'border-box',
                    paddingLeft: '20px',
                    paddingRight: '20px',
                    paddingTop: '0',
                    paddingBottom: '0',
                    display: 'flex',
                    alignItems: 'center',
                }) + ` ${isHoverd ? 'cell--hovered' : ''}`
            }
            style={style}
            onMouseEnter={() => {
                setIsHovered(true)
                data.onRowMouseEnter(rowIndex, data.rows[rowIndex - 1])
            }}
            onMouseLeave={() => setIsHovered(false)}
        >
            <Cell
                value={value}
                onSelect={
                    data.isSelectable && columnIndex === 0 ? () => data.onSelectOne(data.rows[rowIndex - 1]) : undefined
                }
                onAsyncChange={async (v: any) => {
                    const cellData = data?.columns[columnIndex]
                    await cellData?.onAsyncChange?.(v, columnIndex, rowIndex - 1)
                }}
                isSelected={data.isRowSelected(data.rows[rowIndex - 1].id)}
                textQuery={data.textQuery}
                x={columnIndex}
                y={rowIndex - 1}
            />
        </div>
    )
}

function compareCellPlacement(prevProps: any, nextProps: any) {
    // header cells are not rendered through this component
    if (prevProps.rowIndex === 0) {
        return true
    }

    if (prevProps.normalizedWidths !== nextProps.normalizedWidths) {
        return false
    }

    if (
        prevProps.data.columns !== nextProps.data.columns ||
        prevProps.data.rows !== nextProps.data.rows ||
        prevProps.style !== nextProps.style
    ) {
        return false
    }

    if (
        prevProps.data.isSelectable === nextProps.data.isSelectable &&
        prevProps.data.columnHighlightIndex === nextProps.data.columnHighlightIndex &&
        prevProps.data.rowHighlightIndex === nextProps.data.rowHighlightIndex &&
        prevProps.data.textQuery === nextProps.data.textQuery &&
        prevProps.data.isRowSelected === nextProps.data.isRowSelected
    ) {
        return true
    }

    // at this point we know that the rowHighlightIndex or the columnHighlightIndex has changed.
    // row does not need to re-render if not transitioning _from_ or _to_ highlighted
    // also ensures that all cells are invalidated on column-header hover
    if (
        prevProps.rowIndex !== prevProps.data.rowHighlightIndex &&
        prevProps.rowIndex !== nextProps.data.rowHighlightIndex &&
        prevProps.data.columnHighlightIndex === nextProps.data.columnHighlightIndex &&
        prevProps.data.isRowSelected === nextProps.data.isRowSelected
    ) {
        return true
    }

    // similar to the row highlight optimization, do not update the cell if not in the previously
    // highlighted column or next highlighted.
    if (
        prevProps.columnIndex !== prevProps.data.columnHighlightIndex &&
        prevProps.columnIndex !== nextProps.data.columnHighlightIndex &&
        prevProps.data.rowHighlightIndex === nextProps.data.rowHighlightIndex &&
        prevProps.data.isRowSelected === nextProps.data.isRowSelected
    ) {
        return true
    }

    return false
}

// @ts-ignore
const CellPlacementMemo = React.memo<CellPlacementPropsT, unknown>(({ index, style = {}, data }) => {
    const columns = data.columns.map((v, columnIndex) => ({
        ...v,
        index: columnIndex,
    }))
    const { normalizedWidths } = data

    const renderer = useCallback(
        (column: ColumnT & { index: number }) => {
            const columnIndex = column.index

            return (
                <CellPlacement
                    key={`${columnIndex}:${index}`}
                    columnIndex={columnIndex}
                    rowIndex={index}
                    data={data}
                    // @ts-ignore
                    style={{
                        width: normalizedWidths[columnIndex],
                        background: '#FFF',
                        borderBottom: '1px solid #EEF1F6',
                    }}
                />
            )
        },
        [data, normalizedWidths, index]
    )

    const cellsLeft = React.useMemo(() => {
        return columns.filter((v) => v.pin === 'LEFT').map(renderer)
    }, [columns, renderer])

    const cells = React.useMemo(() => {
        return columns.filter((v) => !v.pin).map(renderer)
    }, [columns, renderer])

    return (
        <div
            key={index}
            // @ts-ignore
            style={{
                ...style,
                display: 'flex',
                width: 'fix-content',
                breakInside: 'avoid',
            }}
        >
            {cellsLeft.length > 0 && (
                <div
                    style={{
                        position: 'sticky',
                        float: 'left',
                        left: 0,
                        borderLeft: '0',
                        borderRight: '1px solid #CFD7E6',
                        display: 'flex',
                        width: 'fix-content',
                        breakInside: 'avoid',
                    }}
                >
                    {cellsLeft}
                </div>
            )}
            <div
                style={{
                    display: 'flex',
                    width: 'fix-content',
                    breakInside: 'avoid',
                }}
            >
                {cells}
            </div>
        </div>
    )
}, compareCellPlacement)

CellPlacementMemo.displayName = 'CellPlacement'

const HeaderContext = React.createContext<HeaderContextT>({
    columns: [],
    columnHighlightIndex: -1,
    emptyMessage: '',
    filters: new Map(),
    loading: false,
    loadingMessage: '',
    isScrollingX: false,
    isSelectable: false,
    isSelectedAll: false,
    isSelectedIndeterminate: false,
    measuredWidths: [],
    onMouseEnter: () => {},
    onMouseLeave: () => {},
    onResize: () => {},
    onSelectMany: () => {},
    onSelectNone: () => {},
    onSort: () => {},
    resizableColumnWidths: false,
    rowActions: [],
    rowHeight: 0,
    rowHighlightIndex: -1,
    rows: [],
    scrollLeft: 0,
    sortIndex: -1,
    sortDirection: null,
    tableHeight: 0,
    widths: [],
})
HeaderContext.displayName = 'HeaderContext'

type HeaderProps = {
    columnTitle: string
    hoverIndex: number
    index: number
    isSortable: boolean
    isSelectable: boolean
    isSelectedAll: boolean
    isSelectedIndeterminate: boolean
    onMouseEnter: (num: number) => void
    onMouseLeave: () => void
    onResize: (columnIndex: number, delta: number) => void
    onResizeIndexChange: (columnIndex: number) => void
    onSelectMany: () => void
    onSelectNone: () => void
    onSort: () => void
    resizableColumnWidths: boolean
    resizeIndex: number
    resizeMaxWidth: number
    resizeMinWidth: number
    sortIndex: number
    sortDirection: SortDirectionsT
    tableHeight: number
}
function Header(props: HeaderProps) {
    const [css, theme] = useStyletron()
    const [startResizePos, setStartResizePos] = React.useState(0)
    const [endResizePos, setEndResizePos] = React.useState(0)
    // flowlint-next-line unclear-type:off
    const headerCellRef = React.useRef<any>(null)

    const RULER_OFFSET = 2
    const isResizingThisColumn = props.resizeIndex === props.index
    const isResizing = props.resizeIndex >= 0

    // @ts-ignore
    function getPositionX(el) {
        if (IS_BROWSER) {
            const rect = el.getBoundingClientRect()
            return rect.left + window.scrollX
        }
        return 0
    }

    React.useLayoutEffect(() => {
        function handleMouseMove(event: MouseEvent) {
            if (isResizingThisColumn) {
                event.preventDefault()

                if (headerCellRef.current) {
                    const left = getPositionX(headerCellRef.current)
                    const width = event.clientX - left - 5
                    const max = Math.ceil(props.resizeMaxWidth)
                    const min = Math.ceil(props.resizeMinWidth)

                    if (min === max) {
                        return
                    }

                    if (width >= min && width <= max) {
                        setEndResizePos(event.clientX - RULER_OFFSET)
                    }
                    if (width < min) {
                        setEndResizePos(left + min - RULER_OFFSET)
                    }
                    if (width > max) {
                        setEndResizePos(max - width - RULER_OFFSET)
                    }
                }
            }
        }

        function handleMouseUp() {
            props.onResize(props.index, endResizePos - startResizePos)
            props.onResizeIndexChange(-1)
            setStartResizePos(0)
            setEndResizePos(0)
        }

        if (IS_BROWSER) {
            if (isResizingThisColumn) {
                document.addEventListener('mousemove', handleMouseMove)
                document.addEventListener('mouseup', handleMouseUp)
            }
        }
        return () => {
            if (IS_BROWSER) {
                document.removeEventListener('mousemove', handleMouseMove)
                document.removeEventListener('mouseup', handleMouseUp)
            }
        }
    }, [
        props,
        isResizingThisColumn,
        setEndResizePos,
        setStartResizePos,
        props.onResize,
        props.onResizeIndexChange,
        props.index,
        endResizePos,
        startResizePos,
    ])

    return (
        <>
            <HeaderCell
                ref={headerCellRef}
                // @ts-ignore
                index={props.index}
                sortable={props.isSortable}
                isHovered={!isResizing && props.hoverIndex === props.index}
                isSelectable={props.isSelectable && props.index === 0}
                isSelectedAll={props.isSelectedAll}
                isSelectedIndeterminate={props.isSelectedIndeterminate}
                onMouseEnter={() => {
                    if (!isResizing) {
                        props.onMouseEnter(props.index)
                    }
                }}
                onMouseLeave={() => {
                    if (!isResizing) {
                        props.onMouseLeave()
                    }
                }}
                onSelectAll={props.onSelectMany}
                onSelectNone={props.onSelectNone}
                onSort={props.onSort}
                sortDirection={props.sortIndex === props.index ? props.sortDirection : null}
                title={props.columnTitle}
            />
            {props.resizableColumnWidths && (
                <div
                    className={css({
                        position: 'relative',
                        display: 'flex',
                        alignItems: 'center',
                    })}
                >
                    <div
                        role='presentation'
                        onMouseDown={(event) => {
                            props.onResizeIndexChange(props.index)
                            const x = getPositionX(event.target)
                            setStartResizePos(x)
                            setEndResizePos(x)
                        }}
                        className={css({
                            // @ts-ignore
                            'backgroundColor': isResizingThisColumn ? theme.colors.contentPrimary : null,
                            'cursor': 'ew-resize',
                            'position': 'absolute',
                            'height': '100%',
                            'width': '3px',
                            ':hover': {
                                backgroundColor: theme.colors.contentPrimary,
                            },
                        })}
                        style={{
                            right: `${(RULER_OFFSET + endResizePos - startResizePos) * -1}px`,
                        }}
                    >
                        {isResizingThisColumn && (
                            <div
                                className={css({
                                    backgroundColor: theme.colors.contentPrimary,
                                    position: 'absolute',
                                    height: `${props.tableHeight}px`,
                                    right: '1px',
                                    width: '1px',
                                })}
                            />
                        )}
                    </div>
                </div>
            )}
        </>
    )
}

function Headers() {
    const [css, theme] = useStyletron()
    const locale = React.useContext(LocaleContext)
    const ctx = React.useContext(HeaderContext)
    const [resizeIndex, setResizeIndex] = React.useState(-1)

    const $columns = ctx.columns.map((v, index) => ({
        ...v,
        index,
    }))

    const headerRender = useCallback(
        (column: ColumnT & { index: number }) => {
            const activeFilter = ctx.filters ? ctx.filters.get(column.title) : null
            const columnIndex = column.index
            return (
                <>
                    <Tooltip
                        key={columnIndex}
                        placement={PLACEMENT.bottomLeft}
                        isOpen={ctx.columnHighlightIndex === columnIndex && Boolean(activeFilter)}
                        content={() => {
                            return (
                                <div>
                                    <p
                                        className={css({
                                            ...theme.typography.font100,
                                            color: theme.colors.contentInversePrimary,
                                        })}
                                    >
                                        {locale.datatable.filterAppliedTo} {column.title}
                                    </p>
                                    {activeFilter && (
                                        <p
                                            className={css({
                                                ...theme.typography.font150,
                                                color: theme.colors.contentInversePrimary,
                                            })}
                                        >
                                            {activeFilter.description}
                                        </p>
                                    )}
                                </div>
                            )
                        }}
                    >
                        <div
                            className={css({
                                ...theme.borders.border200,
                                backgroundColor: theme.colors.backgroundPrimary,
                                borderTop: 'none',
                                borderLeft: 'none',
                                border: 'none',
                                // @ts-ignore
                                // borderRight: columnIndex === ctx.columns.length - 1 ? 'none' : '1px solid #e6e6e6',
                                boxSizing: 'border-box',
                                display: 'flex',
                            })}
                            style={{ width: ctx.widths[columnIndex] }}
                        >
                            <Header
                                columnTitle={column.title}
                                hoverIndex={ctx.columnHighlightIndex}
                                index={columnIndex}
                                isSortable={column.sortable}
                                isSelectable={ctx.isSelectable}
                                isSelectedAll={ctx.isSelectedAll}
                                isSelectedIndeterminate={ctx.isSelectedIndeterminate}
                                onMouseEnter={ctx.onMouseEnter}
                                onMouseLeave={ctx.onMouseLeave}
                                onResize={ctx.onResize}
                                onResizeIndexChange={setResizeIndex}
                                onSelectMany={ctx.onSelectMany}
                                onSelectNone={ctx.onSelectNone}
                                onSort={() => ctx.onSort(columnIndex)}
                                resizableColumnWidths={ctx.resizableColumnWidths}
                                resizeIndex={resizeIndex}
                                resizeMinWidth={ctx.measuredWidths[columnIndex]}
                                resizeMaxWidth={column.maxWidth || Infinity}
                                sortIndex={ctx.sortIndex}
                                sortDirection={ctx.sortDirection}
                                tableHeight={ctx.tableHeight}
                            />
                        </div>
                    </Tooltip>
                </>
            )
        },
        [ctx, setResizeIndex, resizeIndex, css, locale, theme]
    )

    const headersLeft = React.useMemo(() => {
        return $columns.filter((v) => v.pin === 'LEFT').map(headerRender)
    }, [$columns, headerRender])

    const headers = React.useMemo(() => {
        return $columns.filter((v) => !v.pin).map(headerRender)
    }, [$columns, headerRender])

    return (
        <div
            className={css({
                position: 'sticky',
                top: 0,
                left: 0,
                width: `${sum(ctx.widths)}px`,
                height: `${HEADER_ROW_HEIGHT}px`,
                display: 'flex',
                // this feels bad.. the absolutely positioned children elements
                // stack on top of this element with the layer component.
                zIndex: 2,
            })}
        >
            {headersLeft.length > 0 && (
                <div
                    style={{
                        position: 'sticky',
                        float: 'left',
                        left: 0,
                        borderLeft: '0',
                        borderRight: '1px solid #CFD7E6',
                        display: 'flex',
                        width: 'fix-content',
                        breakInside: 'avoid',
                    }}
                >
                    {headersLeft}
                </div>
            )}
            <div
                style={{
                    display: 'flex',
                    width: 'fix-content',
                    breakInside: 'avoid',
                }}
            >
                {headers}
            </div>
        </div>
    )
}
// @ts-ignore
function LoadingOrEmptyMessage(props) {
    const [css, theme] = useStyletron()
    return (
        <p
            className={css({
                ...theme.typography.ParagraphSmall,
                color: theme.colors.contentPrimary,
                marginLeft: theme.sizing.scale500,
            })}
        >
            {typeof props.children === 'function' ? props.children() : String(props.children)}
        </p>
    )
}

// replaces the content of the virtualized window with contents. in this case,
// we are prepending a table header row before the table rows (children to the fn).
const InnerTableElement = React.forwardRef<{ children: React.ReactNode; style: Record<string, any> }, HTMLDivElement>(
    (props, ref) => {
        const [, theme] = useStyletron()
        const ctx = React.useContext(HeaderContext)

        // no need to render the cells until the columns have been measured
        if (!ctx.widths.filter(Boolean).length) {
            return null
        }

        const RENDERING = 0
        const LOADING = 1
        const EMPTY = 2
        let viewState = RENDERING
        if (ctx.loading) {
            viewState = LOADING
        } else if (ctx.rows.length === 0) {
            viewState = EMPTY
        }

        const highlightedRow = ctx.rows[ctx.rowHighlightIndex - 1]

        return (
            // @ts-ignore
            <div ref={ref} data-baseweb='data-table' style={props.style}>
                <Headers />

                {viewState === LOADING && <LoadingOrEmptyMessage>{ctx.loadingMessage}</LoadingOrEmptyMessage>}

                {viewState === EMPTY && <LoadingOrEmptyMessage>{ctx.emptyMessage}</LoadingOrEmptyMessage>}

                {viewState === RENDERING && props.children}

                {ctx.rowActions &&
                    Boolean(ctx.rowActions.length) &&
                    ctx.rowHighlightIndex > 0 &&
                    Boolean(highlightedRow) &&
                    !ctx.isScrollingX && (
                        <div
                            style={{
                                alignItems: 'center',
                                backgroundColor: theme.colors.backgroundTertiary,
                                display: 'flex',
                                height: `${ctx.rowHeight}px`,
                                padding: '0 16px',
                                paddingLeft: theme.sizing.scale300,
                                paddingRight: theme.sizing.scale300,
                                position: 'absolute',
                                right: theme.direction !== 'rtl' ? 0 - ctx.scrollLeft : 'initial',
                                left: theme.direction === 'rtl' ? 0 : 'initial',
                                top: (ctx.rowHighlightIndex - 1) * ctx.rowHeight + HEADER_ROW_HEIGHT,
                            }}
                        >
                            {(typeof ctx.rowActions === 'function'
                                ? ctx.rowActions(highlightedRow)
                                : ctx.rowActions
                            ).map((rowAction) => {
                                if (rowAction.renderButton) {
                                    const RowActionButton = rowAction.renderButton
                                    // @ts-ignore
                                    return <RowActionButton />
                                }

                                const RowActionIcon = rowAction.renderIcon
                                return (
                                    <Button
                                        // @ts-ignore
                                        alt={rowAction.label}
                                        key={rowAction.label}
                                        onClick={(event) =>
                                            rowAction.onClick({
                                                event,
                                                row: ctx.rows[ctx.rowHighlightIndex - 1],
                                            })
                                        }
                                        size={BUTTON_SIZES.compact}
                                        kind={BUTTON_KINDS.tertiary}
                                        shape={BUTTON_SHAPES.round}
                                        overrides={{
                                            BaseButton: {
                                                style: {
                                                    marginLeft: theme.sizing.scale300,
                                                    paddingTop: theme.sizing.scale100,
                                                    paddingRight: theme.sizing.scale100,
                                                    paddingBottom: theme.sizing.scale100,
                                                    paddingLeft: theme.sizing.scale100,
                                                },
                                            },
                                        }}
                                    >
                                        {/* @ts-ignore */}
                                        <RowActionIcon size={24} />
                                    </Button>
                                )
                            })}
                        </div>
                    )}
            </div>
        )
    }
)
InnerTableElement.displayName = 'InnerTableElement'

// @ts-ignore
function MeasureScrollbarWidth(props) {
    const [css] = useStyletron()
    const outerRef = React.useRef()
    const innerRef = React.useRef()
    React.useEffect(() => {
        if (outerRef.current && innerRef.current) {
            // @ts-ignore
            const width = outerRef.current.offsetWidth - innerRef.current.offsetWidth
            props.onWidthChange(width)
        }
    }, [props])
    return (
        <div
            className={css({
                height: 0,
                visibility: 'hidden',
                overflow: 'scroll',
            })}
            // @ts-ignore
            ref={outerRef}
        >
            {/* @ts-ignore */}
            <div ref={innerRef} />
        </div>
    )
}

export function DataTable({
    batchActions,
    columns,
    filters,
    emptyMessage,
    loading,
    loadingMessage,
    onIncludedRowsChange,
    onRowHighlightChange,
    onSelectMany,
    onSelectNone,
    onSelectOne,
    onSort,
    resizableColumnWidths = false,
    rows: allRows,
    rowActions = [],
    rowHeight = 44,
    rowHighlightIndex: rowHighlightIndexControlled,
    selectedRowIds,
    sortIndex,
    sortDirection,
    textQuery = '',
    controlRef,
}: DataTablePropsT) {
    const [, theme] = useStyletron()
    const locale = React.useContext(LocaleContext)

    const rowHeightAtIndex = React.useCallback(
        (index) => {
            if (index === 0) {
                return HEADER_ROW_HEIGHT
            }
            return rowHeight
        },
        [rowHeight]
    )

    // We use state for our ref, to allow hooks to  update when the ref changes.
    // flowlint-next-line unclear-type:off
    const [gridRef, setGridRef] = React.useState<VariableSizeGrid<any> | null>(null)
    const [measuredWidths, setMeasuredWidths] = React.useState(columns.map(() => 0))
    const [resizeDeltas, setResizeDeltas] = React.useState(columns.map(() => 0))
    React.useEffect(() => {
        setMeasuredWidths((prev) => {
            return columns.map((v, index) => prev[index] || 0)
        })
        setResizeDeltas((prev) => {
            return columns.map((v, index) => prev[index] || 0)
        })
    }, [columns])

    const resetAfterColumnIndex = React.useCallback(
        (columnIndex) => {
            if (gridRef) {
                // for grid
                // gridRef.resetAfterColumnIndex?.(columnIndex, true)
                // @ts-ignore
                gridRef.resetAfterIndex?.(columnIndex, true)
            }
        },
        [gridRef]
    )
    const handleWidthsChange = React.useCallback(
        (nextWidths) => {
            setMeasuredWidths(nextWidths)
            resetAfterColumnIndex(0)
        },
        [setMeasuredWidths, resetAfterColumnIndex]
    )
    const handleColumnResize = React.useCallback(
        (columnIndex, delta) => {
            setResizeDeltas((prev) => {
                // eslint-disable-next-line no-param-reassign
                prev[columnIndex] = Math.max(prev[columnIndex] + delta, 0)
                return [...prev]
            })
            resetAfterColumnIndex(columnIndex)
        },
        [setResizeDeltas, resetAfterColumnIndex]
    )

    const [scrollLeft, setScrollLeft] = React.useState(0)
    const [isScrollingX, setIsScrollingX] = React.useState(false)
    const [recentlyScrolledX, setRecentlyScrolledX] = React.useState(false)
    React.useLayoutEffect(() => {
        if (recentlyScrolledX !== isScrollingX) {
            setIsScrollingX(recentlyScrolledX)
        }

        if (recentlyScrolledX) {
            const timeout = setTimeout(() => {
                setRecentlyScrolledX(false)
            }, 200)
            return () => clearTimeout(timeout)
        }
        return () => {}
    }, [recentlyScrolledX, isScrollingX])
    const handleScroll = React.useCallback(
        (params) => {
            setScrollLeft(params.scrollLeft)
            if (params.scrollLeft !== scrollLeft) {
                setRecentlyScrolledX(true)
            }
        },
        [scrollLeft, setScrollLeft, setRecentlyScrolledX]
    )

    const sortedIndices = React.useMemo(() => {
        const toSort = allRows.map((r, i) => [r, i])
        const index = sortIndex

        if (index !== null && index !== undefined && index !== -1 && columns[index]) {
            const { sortFn } = columns[index]
            // @ts-ignore
            const getValue = (row) => columns[index].mapDataToValue(row.data)
            if (sortDirection === SORT_DIRECTIONS.ASC) {
                toSort.sort((a, b) => sortFn(getValue(a[0]), getValue(b[0])))
            } else if (sortDirection === SORT_DIRECTIONS.DESC) {
                toSort.sort((a, b) => sortFn(getValue(b[0]), getValue(a[0])))
            }
        }

        return toSort.map((el) => el[1])
    }, [sortIndex, sortDirection, columns, allRows])

    const filteredIndices = React.useMemo(() => {
        const set = new Set(allRows.map((_, idx) => idx))
        // @ts-ignore
        Array.from(filters || new Set(), (f) => f).forEach(([title, filter]) => {
            const columnIndex = columns.findIndex((c) => c.title === title)
            const column = columns[columnIndex]
            if (!column) {
                return
            }

            const filterFn = column.buildFilter(filter)
            Array.from(set).forEach((idx) => {
                if (!filterFn(column.mapDataToValue(allRows[idx].data))) {
                    set.delete(idx)
                }
            })
        })

        if (textQuery) {
            // @ts-ignore
            const stringishColumnIndices = []
            for (let i = 0; i < columns.length; i++) {
                if (columns[i].textQueryFilter) {
                    stringishColumnIndices.push(i)
                }
            }
            Array.from(set).forEach((idx) => {
                // @ts-ignore
                const matches = stringishColumnIndices.some((cdx) => {
                    const column = columns[cdx]
                    const { textQueryFilter } = column
                    if (textQueryFilter) {
                        return textQueryFilter(textQuery, column.mapDataToValue(allRows[idx].data))
                    }
                    return false
                })

                if (!matches) {
                    set.delete(idx)
                }
            })
        }

        return set
    }, [filters, textQuery, columns, allRows])

    const rows = React.useMemo(() => {
        // @ts-ignore
        const result = sortedIndices.filter((idx) => filteredIndices.has(idx)).map((idx) => allRows[idx])

        if (onIncludedRowsChange) {
            onIncludedRowsChange(result)
        }
        return result
    }, [sortedIndices, filteredIndices, onIncludedRowsChange, allRows])

    React.useImperativeHandle(controlRef, () => ({ getRows: () => rows }), [rows])

    const [browserScrollbarWidth, setBrowserScrollbarWidth] = React.useState(0)
    const normalizedWidths = React.useMemo(() => {
        const resizedWidths = measuredWidths.map((w, i) => Math.floor(w) + Math.floor(resizeDeltas[i]))
        if (gridRef) {
            const gridProps = gridRef.props

            let isContentTallerThanContainer = false
            let visibleRowHeight = 0
            for (let rowIndex = 0; rowIndex < rows.length; rowIndex++) {
                visibleRowHeight += rowHeightAtIndex(rowIndex)
                if (visibleRowHeight >= gridProps.height) {
                    isContentTallerThanContainer = true
                    break
                }
            }

            const scrollbarWidth = isContentTallerThanContainer ? browserScrollbarWidth : 0

            const remainder = gridProps.width - sum(resizedWidths) - scrollbarWidth
            const padding = Math.floor(remainder / columns.filter((c) => (c ? c.fillWidth : true)).length)
            if (padding > 0) {
                const result = []
                // -1 so that we loop over all but the last item
                for (let i = 0; i < resizedWidths.length - 1; i++) {
                    if (columns[i] && columns[i].fillWidth) {
                        result.push(resizedWidths[i] + padding)
                    } else {
                        result.push(resizedWidths[i])
                    }
                }
                result.push(gridProps.width - sum(result) - scrollbarWidth - 2)
                resetAfterColumnIndex(0)
                return result
            }
        }
        return resizedWidths
    }, [
        gridRef,
        measuredWidths,
        resizeDeltas,
        browserScrollbarWidth,
        rows.length,
        columns,
        resetAfterColumnIndex,
        rowHeightAtIndex,
    ])

    const isSelectable = batchActions ? !!batchActions.length : false
    const isSelectedAll = React.useMemo(() => {
        if (!selectedRowIds) {
            return false
        }
        return !!rows.length && selectedRowIds.size >= rows.length
    }, [selectedRowIds, rows.length])
    const isSelectedIndeterminate = React.useMemo(() => {
        if (!selectedRowIds) {
            return false
        }
        return !!selectedRowIds.size && selectedRowIds.size < rows.length
    }, [selectedRowIds, rows.length])
    const isRowSelected = React.useCallback(
        (id) => {
            if (selectedRowIds) {
                return selectedRowIds.has(id)
            }
            return false
        },
        [selectedRowIds]
    )
    const handleSelectMany = React.useCallback(() => {
        if (onSelectMany) {
            onSelectMany(rows)
        }
    }, [rows, onSelectMany])
    const handleSelectNone = React.useCallback(() => {
        if (onSelectNone) {
            onSelectNone()
        }
    }, [onSelectNone])
    const handleSelectOne = React.useCallback(
        (row) => {
            if (onSelectOne) {
                onSelectOne(row)
            }
        },
        [onSelectOne]
    )

    const handleSort = React.useCallback(
        (columnIndex) => {
            if (onSort) {
                onSort(columnIndex)
            }
        },
        [onSort]
    )

    const [columnHighlightIndex, setColumnHighlightIndex] = React.useState(-1)
    const [rowHighlightIndex, setRowHighlightIndex] = React.useState(-1)

    // @ts-ignore
    const handleRowHighlightIndexChange = useCallback(
        (nextIndex) => {
            setRowHighlightIndex(nextIndex)
            if (gridRef) {
                if (nextIndex >= 0) {
                    // $FlowFixMe - unable to get react-window types
                    // gridRef.scrollToItem({ rowIndex: nextIndex })
                }
                onRowHighlightChange?.(nextIndex, rows[nextIndex - 1])
            }
        },
        [setRowHighlightIndex, onRowHighlightChange, gridRef, rows]
    )

    const handleRowMouseEnter = React.useCallback(
        (nextIndex) => {
            setColumnHighlightIndex(-1)
            if (nextIndex !== rowHighlightIndex) {
                // handleRowHighlightIndexChange(nextIndex)
            }
        },
        [rowHighlightIndex]
    )
    // @ts-ignore
    function handleColumnHeaderMouseEnter(columnIndex) {
        setColumnHighlightIndex(columnIndex)
        handleRowHighlightIndexChange(-1)
    }
    function handleColumnHeaderMouseLeave() {
        setColumnHighlightIndex(-1)
    }

    React.useEffect(() => {
        if (typeof rowHighlightIndexControlled === 'number') {
            handleRowHighlightIndexChange(rowHighlightIndexControlled)
        }
    }, [rowHighlightIndexControlled, handleRowHighlightIndexChange])

    const itemData = React.useMemo(() => {
        return {
            columnHighlightIndex,
            // warning: this can cause performance problem, and inline edit will have wrong behaviour so use row own behaviour
            // rowHighlightIndex,
            isRowSelected,
            isSelectable,
            onRowMouseEnter: handleRowMouseEnter,
            onSelectOne: handleSelectOne,
            columns,
            rows,
            textQuery,
            normalizedWidths,
        }
    }, [
        handleRowMouseEnter,
        columnHighlightIndex,
        isRowSelected,
        isSelectable,
        // rowHighlightIndex,
        rows,
        columns,
        handleSelectOne,
        textQuery,
        normalizedWidths,
    ])

    return (
        <>
            <MeasureColumnWidths
                columns={columns}
                rows={rows}
                // widths={measuredWidths}
                isSelectable={isSelectable}
                onWidthsChange={handleWidthsChange}
            />
            {/* @ts-ignore */}
            <MeasureScrollbarWidth onWidthChange={(w) => setBrowserScrollbarWidth(w)} />
            <AutoSizer>
                {({ height, width }) => (
                    <HeaderContext.Provider
                        value={{
                            columns,
                            columnHighlightIndex,
                            // @ts-ignore
                            emptyMessage: emptyMessage || locale.datatable.emptyState,
                            filters,
                            loading: Boolean(loading),
                            // @ts-ignore
                            loadingMessage: loadingMessage || locale.datatable.loadingState,
                            isScrollingX,
                            isSelectable,
                            isSelectedAll,
                            isSelectedIndeterminate,
                            measuredWidths,
                            onMouseEnter: handleColumnHeaderMouseEnter,
                            onMouseLeave: handleColumnHeaderMouseLeave,
                            onResize: handleColumnResize,
                            onSelectMany: handleSelectMany,
                            onSelectNone: handleSelectNone,
                            onSort: handleSort,
                            resizableColumnWidths,
                            rowActions,
                            rowHeight,
                            rowHighlightIndex,
                            rows,
                            scrollLeft,
                            sortDirection: sortDirection || null,
                            sortIndex: typeof sortIndex === 'number' ? sortIndex : -1,
                            tableHeight: height,
                            widths: normalizedWidths,
                        }}
                    >
                        <VariableSizeList
                            ref={setGridRef as any}
                            overscanRowCount={10}
                            innerElementType={InnerTableElement}
                            height={height}
                            width={width - 2}
                            itemData={itemData}
                            onScroll={handleScroll}
                            itemSize={rowHeightAtIndex}
                            // plus one to account for additional header row
                            itemCount={rows.length + 1}
                            style={{}}
                            direction={theme.direction === 'rtl' ? 'rtl' : 'ltr'}
                        >
                            {/* @ts-ignore */}
                            {CellPlacementMemo}
                        </VariableSizeList>
                    </HeaderContext.Provider>
                )}
            </AutoSizer>
        </>
    )
}
