import React from 'react'
import cn from 'classnames'
import { themedUseStyletron } from '../../theme/styletron'
import { HEADER_ROW_HEIGHT, HeaderContext } from './headers/header'
import CellPlacement from './cells/cell-placement'
import { ColumnT } from './types'
import Headers from './headers/headers'
import { VariableSizeGrid } from '../react-window'
import TableActions from '../../GridTable/components/TableActions'
import { useEventCallback } from '@starwhale/core/utils'
import { useClickAway } from 'ahooks'

function LoadingOrEmptyMessage(props: { children: React.ReactNode | (() => React.ReactNode) }) {
    return (
        <div className='m-auto content-full'>
            {typeof props.children === 'function' ? props.children() : props.children}
        </div>
    )
}

const RENDERING = 0
const LOADING = 1
const EMPTY = 2

// @ts-ignore
const sum = (ns) => ns.reduce((s, n) => s + n, 0)
type InnerTableElementProps = {
    children: React.ReactNode | null
    style: React.CSSProperties
    data: any
    gridRef: typeof VariableSizeGrid
}

type ColumnTmpT = ColumnT & { columnIndex: number }

const filterColumns = (columns: ColumnT[], attr: string, value: any) => {
    const _arr = [] as ColumnTmpT[]
    columns.forEach((column: ColumnT, index) => {
        if (column[attr] === value) {
            _arr.push({
                ...column,
                columnIndex: index,
            })
        }
    })
    return _arr
}

// replaces the content of the virtualized window with contents. in this case,
// we are prepending a table header row before the table rows (children to the fn).
const InnerTableElement = React.forwardRef<HTMLDivElement, InnerTableElementProps>((props, ref) => {
    const [css] = themedUseStyletron()
    const ctx = React.useContext(HeaderContext)
    let viewState = RENDERING
    if (ctx.loading) {
        viewState = LOADING
    } else if (ctx.rows.length === 0) {
        viewState = EMPTY
    }
    const { data, gridRef } = props
    const { rowHighlightIndex = 2 } = ctx

    const $columns = React.useMemo(() => filterColumns(data.columns, 'pin', 'LEFT'), [data.columns])

    const $columnsRight = React.useMemo(() => filterColumns(data.columns, 'pin', 'RIGHT'), [data.columns])

    const $columnsRightWidth = React.useMemo(() => {
        return sum($columnsRight.map((v) => ctx.widths.get(v.key) ?? 0))
    }, [$columnsRight, ctx.widths])

    const pinnedWidth = React.useMemo(
        () => sum(ctx.columns.map((v) => (v.pin === 'LEFT' ? ctx.widths.get(v.key) : 0))),
        [ctx.columns, ctx.widths]
    )

    const list = React.Children.toArray(props.children)
    // @ts-ignore
    const rowStartIndex = list[0]?.props?.rowIndex
    // @ts-ignore
    const rowStopIndex = list[list.length - 1]?.props?.rowIndex

    // notice: must generate by calculate not from children, because pin column or row will not render when scrolling
    const renderPinned = React.useCallback(
        (_columns, { pinRight = false } = {}) => {
            const cells: React.ReactNode[] = []
            if (!gridRef || !rowStopIndex) return cells
            _columns.forEach((column: any) => {
                const { columnIndex } = column
                for (let rowIndex = rowStartIndex; rowIndex <= rowStopIndex; rowIndex++) {
                    // @ts-ignore
                    const { left, ...rest } = gridRef._getItemStyle(rowIndex, columnIndex)
                    cells.push(
                        <CellPlacement
                            key={`${rowIndex}-${columnIndex}`}
                            columnIndex={columnIndex}
                            rowIndex={rowIndex}
                            data={data}
                            style={{
                                ...rest,
                                width: ctx.widths.get(column.key) ?? 0,
                                zIndex: 1,
                                left: pinRight ? undefined : left,
                            }}
                        />
                    )
                }
            })

            return cells
        },
        [$columns, data, props.children, ctx.widths, gridRef, rowStopIndex]
    )

    const $childrenPinnedLeft = React.useMemo(() => {
        return renderPinned($columns)
    }, [renderPinned, $columns])

    const $childrenPinnedRight = React.useMemo(() => {
        return renderPinned($columnsRight, { pinRight: true })
    }, [renderPinned, $columnsRight])

    const [$background, $backgroundPinned] = React.useMemo(() => {
        const cells: React.ReactNode[] = []
        const pinnedCells: React.ReactNode[] = []
        if (!gridRef) return cells
        const list = React.Children.toArray(props.children)
        if (list.length === 0) return cells

        // @ts-ignore
        const rowStartIndex = list[0]?.props?.rowIndex
        // @ts-ignore
        const rowStopIndex = list[list.length - 1]?.props?.rowIndex

        if (
            rowHighlightIndex !== undefined &&
            rowHighlightIndex >= rowStartIndex &&
            rowHighlightIndex <= rowStopIndex
        ) {
            cells.push(
                <div
                    className='table-row-background'
                    key={rowHighlightIndex}
                    style={{
                        // @ts-ignore
                        ...(gridRef._getItemStyle(rowHighlightIndex, 0) ?? {}),
                        width: '100%',
                        backgroundColor: 'rgb(243, 245, 249, 0.8)',
                        marginBottom: '-42px',
                        zIndex: -1,
                        right: 0,
                    }}
                />
            )
            pinnedCells.push(
                <div
                    className='table-row-background'
                    key={rowHighlightIndex}
                    style={{
                        // @ts-ignore
                        ...(gridRef._getItemStyle(rowHighlightIndex, 0) ?? {}),
                        width: '100%',
                        backgroundColor: 'rgb(243, 245, 249, 0.8)',
                        marginBottom: '-42px',
                        zIndex: 0,
                        right: 0,
                    }}
                />
            )
        }

        return [cells, pinnedCells]
    }, [props.children, gridRef, rowHighlightIndex])

    // useIfChanged({
    //     $childrenPinned,
    //     $children,
    //     $background,
    // })

    if (ctx.widths.size === 0) {
        return null
    }

    /* action bar */
    const innerRef = React.useRef<HTMLDivElement | undefined>(undefined)
    const [isFocus, setIsFocus] = React.useState(false)
    const [focusRect, setFocusRect] = React.useState<{ left: number; top: number } | undefined>(undefined)
    const [focusRowIndex, setFocusRowIndex] = React.useState<number | undefined>(undefined)
    const handleClick = useEventCallback((event) => {
        const cell = event.target.closest('[data-row-index]')
        const rowIndex = cell?.getAttribute('data-row-index')
        if (isFocus) {
            setIsFocus(false)
            return
        }
        setIsFocus(true)
        setFocusRowIndex(rowIndex)
        setFocusRect({
            left: event.clientX,
            top: event.clientY,
        })
    })
    useClickAway((e) => {
        // @ts-ignore
        if (e.target?.closest('.table-inner-sticky')) return
        // @ts-ignore
        if (e.target?.closest('.table-inner')) return
        setIsFocus(false)
    }, innerRef)
    const $actions = React.useMemo(() => {
        const rowIndex = isFocus ? focusRowIndex : ctx.rowHighlightIndex
        if (rowIndex === undefined || rowIndex < 0) return undefined
        const row = ctx.rows?.[rowIndex]
        if (!row) return undefined
        const actions = typeof ctx.rowActions === 'function' ? ctx.rowActions(row) : ctx.rowActions
        if (!actions) return undefined

        if (isFocus) {
            return (
                <TableActions
                    key={0}
                    actions={actions}
                    isFocus={isFocus}
                    focusRect={focusRect}
                    rowRect={{}}
                    selectedRowIndex={rowIndex}
                />
            )
        }

        if (!innerRef?.current) return

        return (
            <TableActions
                key={1}
                actions={actions}
                isFocus={isFocus}
                focusRect={focusRect}
                mountNode={innerRef?.current}
                rowRect={{
                    left: ctx.width + ctx.scrollLeft,
                    top: ctx.rowHighlightIndex * ctx.rowHeight,
                }}
                selectedRowIndex={rowIndex}
            />
        )
    }, [
        ctx.width,
        ctx.rows,
        ctx.rowHighlightIndex,
        ctx.rowActions,
        isFocus,
        focusRowIndex,
        focusRect,
        ctx.width,
        ctx.scrollLeft,
        ctx.rowHeight,
        innerRef?.current,
    ])

    const $pinned = (
        <div
            className='table-inner-sticky bg-white sticky z-2 flex h-0 left-0 border-l-0 overflow-visible break-inside-avoid'
            onClick={handleClick}
        >
            {viewState === RENDERING && $columns.length > 0 && (
                <div
                    className='table-columns-pinned relative overflow-hidden'
                    // @ts-ignore
                    style={{
                        ...props.style,
                        width: pinnedWidth,
                        borderRight: '1px solid #CFD7E6',
                    }}
                >
                    {$childrenPinnedLeft}
                    {$backgroundPinned}
                </div>
            )}
            {viewState === RENDERING && $columnsRight.length > 0 && (
                <div
                    className='table-columns-pinned relative overflow-hidden flex-c-c'
                    // @ts-ignore
                    style={{
                        ...props.style,
                        width: $columnsRightWidth,
                        marginLeft: 'auto',
                        marginRight: '-1px',
                        borderLeft: '1px solid #CFD7E6',
                    }}
                >
                    {$childrenPinnedRight}
                    {$backgroundPinned}
                </div>
            )}
        </div>
    )

    return (
        <>
            {$pinned}
            <div
                // @ts-ignore
                ref={innerRef}
                className='table-inner min-w-full absolute flex-1 flex'
                style={props.style}
                onMouseLeave={ctx?.onRowMouseLeave}
                onClick={handleClick}
            >
                {viewState === LOADING && <LoadingOrEmptyMessage>{ctx.loadingMessage as any}</LoadingOrEmptyMessage>}
                {viewState === EMPTY && <LoadingOrEmptyMessage>{ctx.emptyMessage as any}</LoadingOrEmptyMessage>}
                {viewState === RENDERING && props.children}
                {$background}
                {$actions}
            </div>
        </>
    )
})

export default InnerTableElement
