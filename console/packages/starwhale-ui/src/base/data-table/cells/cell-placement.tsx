import React from 'react'
import cn from 'classnames'
import type { ColumnT, RowT } from '../types'
import { themedUseStyletron } from '../../../theme/styletron'

export type CellPlacementPropsT = {
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

function CellPlacement({ columnIndex, rowIndex, data, style }: any) {
    const [css, theme] = themedUseStyletron()

    const column = React.useMemo(() => data.columns[columnIndex] ?? null, [data.columns, columnIndex])
    const { row, rowCount, rowData } = React.useMemo(() => {
        const rowTmp = data.rows[rowIndex]
        const rowCountTmp = data.rows.length
        return {
            row: rowTmp,
            rowCount: rowCountTmp,
            rowData: rowTmp?.data ?? {},
        }
    }, [data.rows, rowIndex])
    // eslint-disable-next-line
    const Cell = React.useMemo(() => column.renderCell ?? null, [column])
    // eslint-disable-next-line
    const value = React.useMemo(() => column.mapDataToValue(rowData), [column, rowData])
    const isSelected = React.useMemo(() => data.isRowSelected(data.rows[rowIndex]?.id), [data, rowIndex])

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
                rowIndex === data.rowHighlightIndex ? 'table-row--hovering' : undefined,

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
            onMouseEnter={() => {
                data.onRowMouseEnter(rowIndex)
            }}
            onMouseLeave={() => {
                data.onRowMouseEnter(-1)
            }}
        >
            <Cell
                value={value}
                data={rowData}
                onSelect={data.isSelectable && columnIndex === 0 ? () => data.onSelectOne(row) : undefined}
                onAsyncChange={async (v: any) => {
                    const cellData = data?.columns[columnIndex]
                    await cellData?.onAsyncChange?.(v, columnIndex, rowIndex)
                }}
                isSelected={isSelected}
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

const CellPlacementMemo = React.memo<CellPlacementPropsT>(CellPlacement, compareCellPlacement)

export default CellPlacementMemo
