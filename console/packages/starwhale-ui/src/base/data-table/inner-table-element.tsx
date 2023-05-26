import React from 'react'
import cn from 'classnames'
import { themedUseStyletron } from '../../theme/styletron'
import { HeaderContext } from './headers/header'
import CellPlacement from './cells/cell-placement'
import { VariableSizeGrid } from 'react-window'
import { ColumnT } from './types'
import _ from 'lodash'

function LoadingOrEmptyMessage(props: { children: React.ReactNode | (() => React.ReactNode) }) {
    const [css, theme] = themedUseStyletron()
    return (
        <div
            className={css({
                ...theme.typography.ParagraphSmall,
                color: theme.colors.contentPrimary,
                marginLeft: theme.sizing.scale500,
            })}
        >
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
    gridRef: VariableSizeGrid
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

    const $columns = React.useMemo(
        () => data.columns.filter((column: ColumnT) => column.pin === 'LEFT'),
        [data.columns]
    )

    const pinnedWidth = React.useMemo(
        () => sum(ctx.columns.map((v) => (v.pin === 'LEFT' ? ctx.widths.get(v.key) : 0))),
        [ctx.columns, ctx.widths]
    )

    // notice: must generate by calculate not from children, cause pin column or row will not render when scrolling
    const $childrenPinned = React.useMemo(() => {
        const cells: React.ReactNode[] = []
        if (!gridRef) return cells
        const list = React.Children.toArray(props.children)
        if (list.length === 0) return cells

        // @ts-ignore
        const rowStartIndex = list[0]?.props?.rowIndex
        // @ts-ignore
        const rowStopIndex = list[list.length - 1]?.props?.rowIndex

        $columns.forEach((column: any, columnIndex: number) => {
            for (let rowIndex = rowStartIndex; rowIndex <= rowStopIndex; rowIndex++) {
                cells.push(
                    <CellPlacement
                        key={`${rowIndex}-${columnIndex}`}
                        columnIndex={columnIndex}
                        rowIndex={rowIndex}
                        data={data}
                        style={{
                            // @ts-ignore
                            ...gridRef._getItemStyle(rowIndex, columnIndex),
                            width: ctx.widths.get(column.key) ?? 0,
                        }}
                    />
                )
            }
        })

        return cells
    }, [$columns, data, props.children, ctx.widths, gridRef])

    const $children = React.useMemo(() => {
        return props.children
    }, [props.children])

    // useIfChanged({
    //     $childrenPinned,
    //     $children,
    // })

    if (ctx.widths.size === 0) {
        return null
    }

    return (
        <>
            <div
                className={cn(
                    'table-inner-sticky',
                    css({
                        position: 'sticky',
                        width: 0,
                        height: 0,
                        left: 0,
                        zIndex: 100,
                        borderLeftWidth: '0',
                        borderRight: '1px solid #CFD7E6',
                        overflow: 'visible',
                        breakInside: 'avoid',
                    })
                )}
            >
                {viewState === RENDERING && $childrenPinned.length > 0 && (
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
                        {$childrenPinned}
                    </div>
                )}
            </div>

            <div
                // @ts-ignore
                ref={ref}
                className='table-inner'
                // @ts-ignore
                style={{
                    ...props.style,
                    minWidth: '100%',
                }}
            >
                {viewState === LOADING && <LoadingOrEmptyMessage>{ctx.loadingMessage}</LoadingOrEmptyMessage>}
                {viewState === EMPTY && <LoadingOrEmptyMessage>{ctx.emptyMessage}</LoadingOrEmptyMessage>}
                {viewState === RENDERING && $children}
            </div>
        </>
    )
})

export default InnerTableElement
