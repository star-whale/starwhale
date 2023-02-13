import React, { useCallback, useMemo } from 'react'
import cn from 'classnames'
import type { ColumnT } from '../types'
import { CellPlacementPropsT } from '../cells/cell-placement'

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

const RowPlacementMemo = React.memo<CellPlacementPropsT>(
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
            [data.columns, data.isRowSelected, data.rows, data.isSelectable, rowIndex, pinned, data.normalizedWidths]
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

export default RowPlacementMemo
