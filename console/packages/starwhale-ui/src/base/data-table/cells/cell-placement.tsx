import React from 'react'
import cn from 'classnames'
import type { ColumnT, RowT } from '../types'
import { themedUseStyletron } from '../../../theme/styletron'
import _ from 'lodash'
import IconFont from '@starwhale/ui/IconFont'
import Button, { ExtendButton } from '@starwhale/ui/Button'
import { IGridState } from '@starwhale/ui/GridTable/types'
import shallow from 'zustand/shallow'
import { useStore } from '@starwhale/ui/GridTable/hooks/useStore'

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
        onPreview?: (row: RowT) => void
        onRemove?: (row: RowT) => void
    }
}

const selector = (s: IGridState) => ({
    queryinline: s.queryinline,
    columnleinline: s.columnleinline,
})

function CellPlacement({ columnIndex, rowIndex, data, style }: any) {
    const { queryinline, columnleinline } = useStore(selector, shallow)

    const [css, theme] = themedUseStyletron()
    const {
        textQuery,
        isSelectable,
        isRowSelected,
        previewable,
        removable,
        onRowMouseEnter,
        onSelectOne,
        rows,
        columns,
        getId,
        onPreview,
        onRemove,
    } = data

    const column = React.useMemo(() => columns[columnIndex] ?? null, [columns, columnIndex])

    if (!column) return <></>

    const { row, rowCount, rowData } = React.useMemo(() => {
        const rowTmp = rows[rowIndex]
        const rowCountTmp = rows.length
        return {
            row: rowTmp,
            rowCount: rowCountTmp,
            rowData: rowTmp?.data ?? {},
        }
    }, [rows, rowIndex])

    const Cell = React.useMemo(() => column.renderCell ?? null, [column])
    const value = React.useMemo(() => column.mapDataToValue(rowData), [column, rowData])
    const isSelected = React.useMemo(() => isRowSelected(row), [row])
    const onSelect = React.useMemo(
        () => (isSelectable && columnIndex === 0 ? () => onSelectOne(row) : undefined),
        [isSelectable, onSelectOne, columnIndex, row]
    )
    const [isFocused, setIsFocused] = React.useState(false)
    const handleDoubleClick = React.useCallback(() => {
        setIsFocused(true)
    }, [setIsFocused])
    const handleBlur = React.useCallback(() => {
        setIsFocused(false)
    }, [setIsFocused])

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
                    'borderTop': 'none',
                    'borderBottom': 'none',
                    'borderRight': 'none',
                    'borderLeft': 'none',
                    'boxSizing': 'border-box',
                    'paddingTop': '0',
                    'paddingBottom': '0',
                    'display': 'flex',
                    'alignItems': 'center',
                    'paddingLeft': '12px',
                    'paddingRight': '12px',
                    'textOverflow': 'ellipsis',
                    'overflow': 'hidden',
                    'position': 'relative',
                    'breakinside': 'avoid',
                    ":hover [data-type='cell-fullscreen']": {
                        display: 'grid',
                    },
                    'minWidth': columnIndex == 0 ? '120px' : 'auto',
                })
            )}
            style={style}
            onBlur={handleBlur}
            onDoubleClick={handleDoubleClick}
            onMouseEnter={() => onRowMouseEnter(rowIndex)}
        >
            {previewable && (
                <div
                    data-type='cell-fullscreen'
                    className={css({
                        'position': 'absolute',
                        'right': '5px',
                        'top': '5px',
                        'backgroundColor': 'rgba(2,16,43,0.60)',
                        'height': '22px',
                        'width': '22px',
                        'borderRadius': '2px',
                        'display': 'none',
                        'color': '#FFF',
                        'cursor': 'pointer',
                        'placeItems': 'center',
                        'paddingTop': '1px',
                        'zIndex': 1,
                        ':hover': {
                            backgroundColor: '#5181E0',
                        },
                    })}
                    role='button'
                    tabIndex={0}
                    onClick={() => {
                        onPreview?.({
                            record: value,
                            columnKey: column.key,
                            column,
                            rowIndex,
                            columnIndex,
                        })
                    }}
                >
                    <IconFont type='fullscreen' size={14} />
                </div>
            )}
            {columnIndex === 0 && (
                <div className='flex gap-8px min-w-auto'>
                    {removable && (
                        <ExtendButton
                            // @ts-ignore
                            style={{
                                marginRight: '8px',
                            }}
                            styleAs={['negative']}
                            icon='item-reduce'
                            onClick={() => onRemove?.(getId(value.record))}
                        />
                    )}
                    {(columnleinline || queryinline) && !isSelectable && !removable && <p className='w-38px' />}
                </div>
            )}
            <Cell
                columnKey={column.key}
                value={value}
                data={rowData}
                pin={column.pin}
                onSelect={onSelect}
                isSelected={isSelected}
                textQuery={textQuery}
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
