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
    onResize: (columnIndex: number, delta: number) => void
    onSelectMany?: () => void
    onSelectNone?: () => void
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
})
HeaderContext.displayName = 'HeaderContext'
type HeaderProps = {
    width: number
    columnTitle: string
    hoverIndex: number
    index: number
    isSortable: boolean
    querySlot: React.ReactNode
    isSelectable: boolean
    isQueryInline?: boolean
    isSelectedAll?: boolean
    isSelectedIndeterminate?: boolean
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
    resizeIndex: number
    resizeMaxWidth: number
    resizeMinWidth: number
    sortIndex: number
    sortDirection: SortDirectionsT
    tableHeight: number
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
                    let width = event.clientX - left
                    const max = Math.ceil(props.resizeMaxWidth)
                    const min = Math.ceil(props.resizeMinWidth)

                    if (min === max) {
                        return
                    }

                    if (width >= min && width <= max) {
                        width -= RULER_OFFSET
                    }
                    if (width < min) {
                        width = min
                    }
                    if (width > max) {
                        width = max
                    }

                    props.onResize(props.index, width - startResizePos)
                    setEndResizePos(width + left)

                    console.log('startResizePos', {
                        endResizePos,
                        left,
                        width,
                        startResizePos,
                        offset: width - startResizePos,
                    })
                }
            }
        }

        function handleMouseUp() {
            props.onResizeIndexChange(-1)
            setStartResizePos(0)
            setEndResizePos(0)
        }

        const mousemove = _.throttle(handleMouseMove, 10)
        const mouseup = _.throttle(handleMouseUp, 200)

        if (isResizingThisColumn) {
            document.addEventListener('mousemove', mousemove)
            document.addEventListener('mouseup', mouseup)
        }
        return () => {
            document.removeEventListener('mousemove', mousemove)
            document.removeEventListener('mouseup', mouseup)
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

    if (isResizingThisColumn)
        console.log(startResizePos, endResizePos, props.resizeIndex, props.index, isResizingThisColumn)

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
                isQueryInline={props.isQueryInline && props.index === 0}
                querySlot={props.querySlot}
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
                            props.onResize(props.index, 0)
                            props.onResizeIndexChange(props.index)
                            const x = getPositionX(event.target)
                            setStartResizePos(props.width)
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
                            'width': '3px',
                            ':hover': {
                                backgroundColor: theme.brandTableHeaderResizer,
                            },
                        })}
                    />
                </div>
            )}
        </>
    )
}

export default Header
