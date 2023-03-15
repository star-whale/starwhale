import React, { useRef } from 'react'
import { useStyletron } from 'baseui'
import HeaderCell from './headers/header-cell'
import type { ColumnT, RowT } from './types'
import { VariableSizeGrid } from 'react-window'
import { useWhatChanged } from '@starwhale/core'

const IS_BROWSER = true
const emptyFunction = () => {}

function MeasureColumn({ sampleIndexes, column, columnIndex, rows, isSelectable, onLayout, isQueryInline }) {
    const [css] = useStyletron()

    const ref = useRef()

    React.useEffect(() => {
        if (IS_BROWSER) {
            if (ref.current) {
                onLayout(columnIndex, ref.current.getBoundingClientRect())
            }
        }
    }, [column, onLayout, columnIndex])

    return (
        <div
            ref={ref}
            className={css({
                display: 'flex',
                flexDirection: 'column',
                width: 'fit-content',
                paddingRight: '20px',
            })}
        >
            <HeaderCell
                index={columnIndex}
                isHovered
                isMeasured
                isSelectedAll={false}
                isSelectedIndeterminate={false}
                onMouseEnter={emptyFunction}
                onMouseLeave={emptyFunction}
                onSelectAll={emptyFunction}
                onSelectNone={emptyFunction}
                onSort={emptyFunction}
                sortable={column.sortable}
                sortDirection={null}
                title={column.title}
                isSelectable={isSelectable}
            />
            {sampleIndexes.map((rowIndex, i) => {
                const Cell = column.renderCell
                return (
                    <Cell
                        key={`measure-${i}`}
                        value={column.mapDataToValue(rows[rowIndex].data)}
                        isSelectable={isSelectable}
                        isQueryInline={isQueryInline}
                        isMeasured
                        sortable={column.sortable}
                        x={0}
                        y={rowIndex}
                    />
                )
            })}
        </div>
    )
}
type MeasureColumnWidthsPropsT = {
    columns: ColumnT[]
    // if selectable, measure the first column with checkbox included
    isSelectable: boolean
    isQueryInline: boolean
    onWidthsChange: (nums: number[]) => void
    rows: RowT[]
    gridRef: VariableSizeGrid<any>
}

const MAX_SAMPLE_SIZE = 20

function generateSampleIndices(inputMin, inputMax, maxSamples) {
    const indices = []
    const queue = [[inputMin, inputMax]]

    while (queue.length > 0) {
        const [min, max] = queue.shift()
        if (indices.length < maxSamples) {
            const pivot = Math.floor((min + max) / 2)
            indices.push(pivot)
            const left = pivot - 1
            const right = pivot + 1
            if (left >= min) {
                queue.push([min, left])
            }
            if (right <= max) {
                queue.push([right, max])
            }
        }
    }

    return indices
}

export default function MeasureColumnWidths({
    columns,
    rows,
    isSelectable,
    isQueryInline,
    onWidthsChange,
    gridRef,
}: MeasureColumnWidthsPropsT) {
    const [css] = useStyletron()

    const [widthMap, setWidthMap] = React.useState(new Map())

    const sampleIndexes = React.useMemo<number[]>(() => {
        const sampleSize = rows.length < MAX_SAMPLE_SIZE ? rows.length : MAX_SAMPLE_SIZE
        // const finishedMeasurementCount = (sampleSize + 1) * columns.length
        return generateSampleIndices(0, rows.length - 1, sampleSize)
    }, [rows])

    const handleDimensionsChange = React.useCallback(
        (columnIndex, dimensions) => {
            const nextWidth = Math.min(
                Math.max(columns[columnIndex].minWidth || 0, widthMap.get(columnIndex) || 0, dimensions.width + 1),
                columns[columnIndex].maxWidth || Infinity
            )
            const prevWidth = widthMap.get(columnIndex) ?? 0

            // console.log(columnIndex, nextWidth, prevWidth, dimensions.width, columns)

            if (nextWidth !== widthMap.get(columnIndex) && Math.abs(nextWidth - prevWidth) > 2) {
                widthMap.set(columnIndex, nextWidth)

                if (columnIndex < columns.length) {
                    setWidthMap(widthMap)
                    onWidthsChange(Array.from(widthMap.values()))
                }

                // 1.Refresh at 100% of done
                // 2. Refresh only when there is a width updating ,and the minised of the width is more than 2px
                // console.log('update', widthMap.size, columns.length, widthMap)
                // if (widthMap.size > columns.length) {
                //     const newMap = Array.from(widthMap).slice(0, columns.length)
                //     console.log('new map', new Map(newMap))
                //     setWidthMap(new Map(newMap))
                //     onWidthsChange(Array.from(newMap.values()))
                // } else if (widthMap.size === columns.length) {
                //     setWidthMap(widthMap)
                //     onWidthsChange(Array.from(widthMap.values()))
                // }
            }
        },
        [columns, onWidthsChange, widthMap]
    )

    const $columns = React.useMemo(() => {
        if (!gridRef) return null

        // const [overscanColumnStartIndex, overscanColumnStopIndex] = gridRef._getHorizontalRangeToRender()

        // console.log('----', overscanColumnStartIndex, overscanColumnStopIndex)

        return columns.map((column, i) => {
            // if (!column.pin && i > overscanColumnStartIndex && i < overscanColumnStopIndex) {
            //     return null
            // }

            return (
                <MeasureColumn
                    key={`${column.title}-${String(i)}`}
                    column={column}
                    rows={rows}
                    isSelectable={isSelectable && i === 0}
                    isQueryInline={isQueryInline && i === 0}
                    onLayout={handleDimensionsChange}
                    columnIndex={i}
                    sampleIndexes={sampleIndexes}
                />
            )
        })
    }, [columns, rows, isSelectable, handleDimensionsChange, sampleIndexes, gridRef])

    console.log($columns?.length, widthMap)

    return (
        // eslint-disable-next-line jsx-a11y/role-supports-aria-props
        <div
            data-type='table-measure-column'
            className={css({
                position: 'absolute',
                overflow: 'hidden',
                height: 0,
            })}
            aria-hidden
            role='none'
        >
            {$columns}
        </div>
    )
}
