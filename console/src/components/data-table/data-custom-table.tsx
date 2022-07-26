import React, { useCallback, useMemo } from 'react'
import { VariableSizeGrid } from 'react-window'
import AutoSizer from 'react-virtualized-auto-sizer'
import { useStyletron } from 'baseui'
import { Tooltip, PLACEMENT } from 'baseui/tooltip'
import cn from 'classnames'
import { SORT_DIRECTIONS } from './constants'
import HeaderCell from './header-cell'
import MeasureColumnWidths from './measure-column-widths'
import type { ColumnT, DataTablePropsT, RowT, SortDirectionsT, RowActionT } from './types'
import { LocaleContext } from './locales'
import { IStore } from './store'

// consider pulling this out to a prop if useful.
const HEADER_ROW_HEIGHT = 54
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
    onNoSelect: (id: any) => void
    onSort: (num: number) => void
    resizableColumnWidths: boolean
    compareable: boolean
    rowActions: RowActionT[] | ((row: RowT) => RowActionT[])
    rowHeight: number
    rowHighlightIndex: number
    rows: RowT[]
    scrollLeft: number
    sortIndex: number
    sortDirection: SortDirectionsT
    tableHeight: number
    widths: number[]
    useStore: IStore
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
        onRowMouseEnter: (num: number) => void
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
    const column = data.columns[columnIndex]
    const row = data.rows[rowIndex]
    const rowCount = data.rows.length

    // eslint-disable-next-line
    const Cell = React.useMemo(() => column.renderCell ?? null, [column])
    // eslint-disable-next-line
    const value = React.useMemo(() => column.mapDataToValue(row?.data), [column, row])

    // console.log('CellPlacement', columnIndex, rowIndex, value)
    return (
        <div
            data-column-index={columnIndex}
            data-row-index={rowIndex}
            className={cn(
                'table-cell',
                rowIndex,
                rowCount,
                rowIndex === 0 ? 'table-cell--first' : undefined,
                rowIndex === rowCount - 1 ? 'table-cell--last' : undefined,

                css({
                    ...theme.borders.border200,
                    borderTop: 'none',
                    borderBottom: 'none',
                    borderRight: 'none',
                    borderLeft: 'none',
                    boxSizing: 'border-box',
                    paddingTop: '0',
                    paddingBottom: '0',
                    display: 'flex',
                    alignItems: 'center',
                    paddingLeft: columnIndex === 0 ? '20px' : '12px',
                    paddingRight: '12px',
                    textOverflow: 'ellipsis',
                    overflow: 'hidden',
                    position: 'relative',
                    breakinside: 'avoid',
                })
            )}
            style={style}
        >
            <Cell
                value={value}
                onSelect={data.isSelectable && columnIndex === 0 ? () => data.onSelectOne(row) : undefined}
                onAsyncChange={async (v: any) => {
                    const cellData = data?.columns[columnIndex]
                    await cellData?.onAsyncChange?.(v, columnIndex, rowIndex)
                }}
                isSelected={data.isRowSelected(data.rows[rowIndex]?.id)}
                textQuery={data.textQuery}
                x={columnIndex}
                y={rowIndex}
            />
        </div>
    )
}

function compareCellPlacement(prevProps: any, nextProps: any) {
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
const RowPlacementMemo: React.ReactComponentElement = React.memo<CellPlacementPropsT, unknown>(
    // @ts-ignore
    ({ pinned, rowIndex, style = {}, data }) => {
        const columns = useMemo(
            () =>
                data.columns.map((v, columnIndex) => ({
                    ...v,
                    index: columnIndex,
                })),
            [data.columns]
        )

        const renderer = useCallback(
            (column: ColumnT & { index: number }) => {
                const columnIndex = column.index

                // don't render pin table if row was a normal row
                if (column.pin === 'LEFT' && !pinned) {
                    return (
                        <div
                            key={`${columnIndex}:${rowIndex}`}
                            style={{
                                width: data.normalizedWidths[columnIndex],
                                borderBottom: '1px solid #EEF1F6',
                            }}
                        />
                    )
                }

                return (
                    <CellPlacement
                        key={`${columnIndex}:${rowIndex}`}
                        columnIndex={columnIndex}
                        rowIndex={rowIndex}
                        data={data}
                        // @ts-ignore
                        style={{
                            width: data.normalizedWidths[columnIndex],
                            borderBottom: '1px solid #EEF1F6',
                        }}
                    />
                )
            },
            // eslint-disable-next-line react-hooks/exhaustive-deps
            [data.columns, data.isRowSelected, data.rows, data.isSelectable, rowIndex, pinned]
        )

        // useWhatChanged([columns, renderer, pinned, ''])

        const cells = React.useMemo(() => {
            return columns.filter((v) => (pinned ? v.pin === 'LEFT' : true)).map(renderer)
        }, [columns, renderer, pinned])

        return (
            <div
                key={rowIndex}
                className={cn('table-row', rowIndex === data.rowHighlightIndex ? 'table-row--hovering' : undefined)}
                // @ts-ignore
                style={{
                    ...style,
                    display: 'flex',
                    breakInside: 'avoid',
                    width: '100%',
                }}
                data-row-index={rowIndex}
                onMouseEnter={() => {
                    data.onRowMouseEnter(rowIndex)
                }}
                onMouseLeave={() => {
                    data.onRowMouseEnter(-1)
                }}
            >
                <div
                    style={{
                        display: 'flex',
                        width: 'fix-content',
                        breakInside: 'avoid',
                    }}
                >
                    {cells}
                </div>
                <div style={{ flex: 1, borderBottom: '1px solid #EEF1F6', minWidth: 0 }} />
            </div>
        )
    },
    compareCellPlacement
)

RowPlacementMemo.displayName = 'RowPlacement'

const HeaderContext = React.createContext<HeaderContextT>({
    columns: [],
    columnHighlightIndex: -1,
    emptyMessage: '',
    filters: [],
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
    onNoSelect: () => {},
    onSort: () => {},
    resizableColumnWidths: false,
    compareable: false,
    rowActions: [],
    rowHeight: 0,
    rowHighlightIndex: -1,
    rows: [],
    scrollLeft: 0,
    sortIndex: -1,
    sortDirection: null,
    tableHeight: 0,
    widths: [],
    useStore: {} as IStore,
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
    onNoSelect: (id: any) => void
    onPin: (id: any, bool: boolean) => void
    onSort: (index: number, sortBy: SortDirectionsT) => void
    isFocus: boolean
    isPin: boolean
    onFocus: (arg: boolean) => void
    resizableColumnWidths: boolean
    compareable: boolean
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
                compareable={props.compareable}
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
                onNoSelect={props.onNoSelect}
                onSort={props.onSort}
                onPin={props.onPin}
                isFocus={props.isFocus}
                isPin={props.isPin}
                onFocus={props.onFocus}
                sortDirection={props.sortIndex === props.index ? props.sortDirection : null}
                title={props.columnTitle}
            />
            {props.resizableColumnWidths && (
                <div
                    data-type='header-resize'
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
function Headers({ width }: { width: number }) {
    const [css, theme] = useStyletron()
    const locale = React.useContext(LocaleContext)
    const ctx = React.useContext(HeaderContext)
    const [resizeIndex, setResizeIndex] = React.useState(-1)

    const $columns = ctx.columns.map((v, index) => ({
        ...v,
        index,
    }))

    const store = ctx.useStore()

    const headerRender = useCallback(
        (column: ColumnT & { index: number }) => {
            const activeFilter = null
            const columnIndex = column.index

            const handleNoSelect = () => {
                store.onNoSelect(column.key as string)
            }

            const handleFocus = () => {
                store.onCompareUpdate({
                    comparePinnedKey: column.key as string,
                })
            }
            const handlePin = (index: number, bool: boolean) => {
                store.onCurrentViewColumnsPin(column.key as string, bool)
            }

            // @ts-ignore
            const handleSort = (index: number, direction: SortDirectionsT) => {
                store.onCurrentViewSort(column.key as string, direction)
            }

            const isFoucs = column.key === store.compare?.comparePinnedKey
            const isPin = !!column.pin

            return (
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
                            </div>
                        )
                    }}
                >
                    <div
                        className={css({
                            ...theme.borders.border200,
                            backgroundColor: theme.colors.backgroundPrimary,
                            borderTop: 'none',
                            borderBottom: 'none',
                            borderRight: 'none',
                            borderLeft: 'none',
                            boxSizing: 'border-box',
                            display: 'flex',
                            height: HEADER_ROW_HEIGHT,
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
                            onNoSelect={handleNoSelect}
                            onPin={handlePin}
                            isFocus={isFoucs}
                            isPin={isPin}
                            onFocus={handleFocus}
                            onSort={handleSort}
                            resizableColumnWidths={ctx.resizableColumnWidths}
                            compareable={ctx.compareable}
                            resizeIndex={resizeIndex}
                            resizeMinWidth={ctx.measuredWidths[columnIndex]}
                            resizeMaxWidth={column.maxWidth || Infinity}
                            sortIndex={ctx.sortIndex}
                            sortDirection={ctx.sortDirection}
                            tableHeight={ctx.tableHeight}
                        />
                    </div>
                </Tooltip>
            )
        },
        [store, ctx, setResizeIndex, resizeIndex, css, locale, theme]
    )

    const headersLeft = React.useMemo(() => {
        return $columns.filter((v) => v.pin === 'LEFT').map(headerRender)
    }, [$columns, headerRender])

    const headersLeftWidth = React.useMemo(() => {
        return sum($columns.map((v, index) => (v.pin === 'LEFT' ? ctx.widths[index] : 0)))
    }, [$columns, ctx.widths])

    const headers = React.useMemo(() => {
        return $columns.filter((v) => v.pin !== 'LEFT').map(headerRender)
    }, [$columns, headerRender])

    return (
        <div
            className={cn(
                'table-headers-wrapper',
                css({
                    position: 'sticky',
                    top: 0,
                    left: 0,
                    display: 'flex',
                    zIndex: 2,
                    backgroundColor: '#F3F5F9',
                    overflow: 'hidden',
                })
            )}
            style={{
                width,
            }}
        >
            <div
                className='table-headers-pinned'
                style={{
                    position: 'sticky',
                    left: 0,
                    zIndex: 50,
                    borderLeftWidth: '0',
                    overflow: 'visible',
                    breakInside: 'avoid',
                    display: 'flex',
                    height: HEADER_ROW_HEIGHT,
                }}
            >
                {headersLeft}
            </div>

            {headers.length > 0 && (
                <>
                    <div
                        className='table-headers'
                        style={{
                            width: '100%',
                            position: 'absolute',
                            left: 0,
                            marginLeft: headersLeftWidth,
                            transform: `translate3d(-${ctx.scrollLeft}px,0px,0px)`,
                        }}
                    >
                        <div
                            style={{
                                display: 'flex',
                                breakInside: 'avoid',
                                width: 'fit-content',
                                height: HEADER_ROW_HEIGHT,
                            }}
                        >
                            {headers}
                        </div>
                    </div>
                    <div
                        style={{
                            flex: '1',
                        }}
                    />
                </>
            )}
        </div>
    )
}
// @ts-ignore
function LoadingOrEmptyMessage(props) {
    const [css, theme] = useStyletron()
    return (
        <div
            className={css({
                ...theme.typography.ParagraphSmall,
                color: theme.colors.contentPrimary,
                marginLeft: theme.sizing.scale500,
            })}
        >
            {typeof props.children === 'function' ? props.children() : String(props.children)}
        </div>
    )
}
// replaces the content of the virtualized window with contents. in this case,
// we are prepending a table header row before the table rows (children to the fn).
const InnerTableElement = React.forwardRef<{ children: React.ReactNode; style: Record<string, any> }, HTMLDivElement>(
    (props, ref) => {
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

        // const highlightedRow = ctx.rows[ctx.rowHighlightIndex]

        // @ts-ignore
        const $children = props.children.map((o) => {
            return {
                ...o,
                props: {
                    ...o.props,
                    pinned: true,
                },
            }
        })

        const pinnedWidth = sum(ctx.columns.map((v, index) => (v.pin === 'LEFT' ? ctx.widths[index] : 0)))

        return (
            <>
                <div
                    style={{
                        position: 'sticky',
                        width: 0,
                        height: 0,
                        left: 0,
                        zIndex: 100,
                        borderLeftWidth: '0',
                        borderRight: '1px solid #CFD7E6',
                        overflow: 'visible',
                        breakInside: 'avoid',
                    }}
                >
                    <div
                        className='table-columns-pinned'
                        // @ts-ignore
                        style={{
                            ...props.style,
                            width: pinnedWidth,
                            position: 'relative',
                            overflow: 'hidden',
                        }}
                    >
                        {viewState === RENDERING && $children}
                    </div>
                </div>

                <div
                    // @ts-ignore
                    ref={ref}
                    data-type='table-inner'
                    // @ts-ignore
                    style={{
                        ...props.style,
                        minWidth: '100%',
                    }}
                >
                    {/* <Headers /> */}
                    {viewState === LOADING && <LoadingOrEmptyMessage>{ctx.loadingMessage}</LoadingOrEmptyMessage>}
                    {viewState === EMPTY && <LoadingOrEmptyMessage>{ctx.emptyMessage}</LoadingOrEmptyMessage>}
                    {viewState === RENDERING && props.children}
                </div>
            </>
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
            data-type='table-measure-scrollbar'
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
    rawColumns,
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
    compareable = false,
    rows: allRows,
    rowActions = [],
    rowHeight = 44,
    rowHighlightIndex: rowHighlightIndexControlled,
    selectedRowIds: $selectedRowIds = new Set(),
    sortIndex,
    sortDirection,
    textQuery = '',
    controlRef,
    useStore,
}: DataTablePropsT) {
    const [, theme] = useStyletron()
    const locale = React.useContext(LocaleContext)

    // TODO remove this
    const selectedRowIds = React.useMemo(() => {
        return new Set(Array.from($selectedRowIds))
    }, [$selectedRowIds])

    const rowHeightAtIndex = React.useCallback(
        // eslint-disable-next-line
        (index) => {
            return rowHeight
        },
        [rowHeight]
    )

    // We use state for our ref, to allow hooks to  update when the ref changes.
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
                gridRef.resetAfterColumnIndex?.(columnIndex, true)
                // @ts-ignore
                // gridRef.resetAfterIndex?.(columnIndex, true)
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
            const eventScrollLeft = params.scrollLeft
            setScrollLeft(eventScrollLeft)
            if (eventScrollLeft !== scrollLeft) {
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

    // only
    const filteredIndices = React.useMemo(() => {
        const set = new Set(allRows.map((_, idx) => idx))

        Array.from(filters || new Set(), (f) => f)
            .filter((v: any) => !v.disable)
            .forEach((filter: any) => {
                const columnIndex = rawColumns.findIndex((c) => c.key === filter.key)
                const column = rawColumns[columnIndex]

                if (!column) {
                    return
                }

                const filterFn = filter?.op.buildFilter(filter) // ?? column.buildFilter(filter)
                Array.from(set).forEach((idx) => {
                    if (!filterFn(column.mapDataToValue(allRows[idx].data), allRows[idx].data, column)) {
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
    }, [filters, textQuery, rawColumns, allRows, columns])

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
            const filledColumnsLen = columns.filter((c) => (c ? c.fillWidth : true)).length
            const padding = filledColumnsLen === 0 ? 0 : Math.floor(remainder / filledColumnsLen)

            // console.log(resizedWidths, remainder, padding)
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
            onSelectMany(rows.map((v) => v.id))
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
                onSelectOne(row.id)
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
                onRowHighlightChange?.(nextIndex, rows[nextIndex])
            }
        },
        [setRowHighlightIndex, onRowHighlightChange, gridRef, rows]
    )

    const handleRowMouseEnter = React.useCallback(
        (nextIndex) => {
            setColumnHighlightIndex(-1)
            if (nextIndex !== rowHighlightIndex) {
                handleRowHighlightIndexChange(nextIndex)
            }
        },
        [rowHighlightIndex, handleRowHighlightIndexChange]
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
            // columnHighlightIndex,
            // warning: this can cause performance problem, and inline edit will have wrong behaviour so use row own behaviour
            rowHighlightIndex,
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
        // columnHighlightIndex,
        isRowSelected,
        isSelectable,
        rowHighlightIndex,
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
                widths={measuredWidths}
                isSelectable={isSelectable}
                onWidthsChange={handleWidthsChange}
            />
            {/* @ts-ignore */}
            <MeasureScrollbarWidth onWidthChange={(w) => setBrowserScrollbarWidth(w)} />
            {/* don't assign with to auto sizer */}
            <AutoSizer className='table-auto-resizer'>
                {({ height, width }) => (
                    <HeaderContext.Provider
                        value={{
                            useStore,
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
                            onNoSelect: handleSelectNone,
                            onSort: handleSort,
                            resizableColumnWidths,
                            compareable,
                            rowActions,
                            rowHeight,
                            rowHighlightIndex: -1,
                            rows,
                            scrollLeft,
                            sortDirection: sortDirection || null,
                            sortIndex: typeof sortIndex === 'number' ? sortIndex : -1,
                            tableHeight: height,
                            widths: normalizedWidths,
                            onSelectOne: handleSelectOne,
                        }}
                    >
                        <Headers width={width} />

                        {/* only one column */}
                        <VariableSizeGrid
                            className='table-columns'
                            ref={setGridRef as any}
                            overscanRowCount={20}
                            innerElementType={InnerTableElement}
                            height={height - HEADER_ROW_HEIGHT}
                            columnWidth={() => sum(normalizedWidths)}
                            columnCount={1}
                            width={width}
                            itemData={itemData}
                            onScroll={handleScroll}
                            // itemSize={rowHeightAtIndex}
                            rowCount={rows.length}
                            rowHeight={rowHeightAtIndex}
                            style={{ overflow: 'auto' }}
                            direction={theme.direction === 'rtl' ? 'rtl' : 'ltr'}
                        >
                            {/* @ts-ignore */}
                            {RowPlacementMemo}
                        </VariableSizeGrid>
                    </HeaderContext.Provider>
                )}
            </AutoSizer>
        </>
    )
}
