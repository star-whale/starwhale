import React from 'react'
import HeaderCell from './header-cell'
import type { ColumnT, DataTablePropsT, RowT, SortDirectionsT, RowActionT } from '../types'
import { themedUseStyletron } from '../../../theme/styletron'
import _ from 'lodash'

export const HEADER_ROW_HEIGHT = 44

export type HeaderContextT = {
    columns: ColumnT[]
    columnHighlightIndex: number
    emptyMessage: string | React.ComponentType<any>
    filters: DataTablePropsT['filters']
    loading: boolean
    loadingMessage: string | React.ComponentType<any>
    isScrollingX: boolean
    isSelectable: boolean
    isQueryInline: boolean
    isSelectedAll?: boolean
    isSelectedIndeterminate?: boolean
    measuredWidths: Map<any, any>
    onMouseEnter: (num: number) => void
    onMouseLeave: () => void
    onRowMouseLeave: () => void
    onResize: (columnIndex: number, delta: number) => void
    onSelectMany?: () => void
    onSelectNone?: () => void
    onNoSelect: (id: any) => void
    onSort: (num: number) => void
    resizableColumnWidths: boolean
    compareable: boolean
    removable: boolean
    rowActions?: RowActionT[] | ((row: RowT) => RowActionT[])
    rowHeight: number
    rowHighlightIndex: number
    rows: RowT[]
    scrollLeft: number
    sortIndex: number
    sortDirection: SortDirectionsT
    tableHeight: number
    widths: Map<any, any>
    wrapperWidth?: number
    scrollbarWidth: number
}

export const HeaderContext = React.createContext<HeaderContextT>({
    columns: [],
    columnHighlightIndex: -1,
    emptyMessage: '',
    filters: [],
    loading: false,
    loadingMessage: '',
    isScrollingX: false,
    isSelectable: false,
    isSelectedAll: false,
    isQueryInline: false,
    isSelectedIndeterminate: false,
    measuredWidths: new Map(),
    onMouseEnter: () => {},
    onMouseLeave: () => {},
    onRowMouseLeave: () => {},
    onResize: () => {},
    onSelectMany: () => {},
    onSelectNone: () => {},
    onNoSelect: () => {},
    onSort: () => {},
    resizableColumnWidths: false,
    compareable: false,
    removable: false,
    rowActions: undefined,
    rowHeight: 0,
    rowHighlightIndex: -1,
    rows: [],
    scrollLeft: 0,
    sortIndex: -1,
    sortDirection: null,
    tableHeight: 0,
    widths: new Map(),
    scrollbarWidth: 0,
})
HeaderContext.displayName = 'HeaderContext'
type HeaderProps = {
    width: number
    columnTitle: string
    hoverIndex: number
    index: number
    isSortable: boolean
    isSelectable: boolean
    isQueryInline?: boolean
    isSelectedAll?: boolean
    isSelectedIndeterminate?: boolean
    selectedRowIds: Set<any>
    onMouseEnter: (num: number) => void
    onMouseLeave: () => void
    onResize: (columnIndex: number, delta: number) => void
    onResizeIndexChange: (columnIndex: number) => void
    onSelectMany?: () => void
    onSelectNone?: () => void
    onNoSelect: (id: any) => void
    onPin: (id: any, bool: boolean) => void
    onSort: (index: number, sortBy: SortDirectionsT) => void
    isFocus: boolean
    isPin: boolean
    onFocus: (arg: boolean) => void
    resizableColumnWidths: boolean
    compareable: boolean
    removable: boolean
    resizeIndex: number
    resizeMaxWidth: number
    resizeMinWidth: number
    sortIndex: number
    sortDirection: SortDirectionsT
    tableHeight: number
    wrapperWidth: number
    rowActions?: RowActionT[] | ((row: RowT) => RowActionT[])
}

function Header(props: HeaderProps) {
    const [css, theme] = themedUseStyletron()
    const [startResizePos, setStartResizePos] = React.useState(0)
    const [endResizePos, setEndResizePos] = React.useState(0)
    // flowlint-next-line unclear-type:off
    const headerCellRef = React.useRef<any>(null)

    const RULER_OFFSET = 2
    const isResizingThisColumn = props.resizeIndex === props.index
    const isResizing = props.resizeIndex >= 0

    // @ts-ignore
    function getPositionX(el) {
        const rect = el.getBoundingClientRect()
        return rect.left + window.scrollX
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
                        setEndResizePos(event.clientX)
                    }
                    if (width < min) {
                        setEndResizePos(left + min)
                    }
                    if (width > max) {
                        setEndResizePos(left + max)
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

        // notice:  no throttle, may cause seq fault
        const mousemove = handleMouseMove
        const mouseup = handleMouseUp

        if (isResizingThisColumn) {
            document.addEventListener('mousemove', mousemove)
            document.addEventListener('mouseup', mouseup)
        }
        return () => {
            document.removeEventListener('mousemove', mousemove)
            document.removeEventListener('mouseup', mouseup)
        }
    }, [
        props.resizeMinWidth,
        props.resizeMaxWidth,
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
                removable={props.removable}
                isHovered={!isResizing && props.hoverIndex === props.index}
                isSelectable={props.isSelectable && props.index === 0}
                isSelectedAll={props.isSelectedAll}
                isSelectedIndeterminate={props.isSelectedIndeterminate}
                selectedRowIds={props.selectedRowIds}
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
                isQueryInline={props.isQueryInline && props.index === 0}
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
                            'backgroundColor':
                                isResizingThisColumn || props.hoverIndex === props.index
                                    ? theme.brandTableHeaderResizer
                                    : null,
                            'cursor': 'ew-resize',
                            'position': 'absolute',
                            'height': '100%',
                            'width': '5px',
                            ':hover': {
                                backgroundColor: theme.brandTableHeaderResizer,
                            },
                        })}
                        style={{
                            right: `${(RULER_OFFSET + endResizePos - startResizePos) * -1}px`,
                        }}
                    >
                        {isResizingThisColumn && (
                            <div
                                className={css({
                                    backgroundColor: theme.brandTableHeaderResizer,
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

export default Header
