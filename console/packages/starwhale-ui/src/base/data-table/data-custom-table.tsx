import React, { useCallback } from 'react'
import { VariableSizeGrid } from 'react-window'
import AutoSizer from 'react-virtualized-auto-sizer'
import { SORT_DIRECTIONS } from './constants'
import MeasureColumnWidths from './measure-column-widths'
import { LocaleContext } from './locales'
import { themedUseStyletron } from '../../theme/styletron'
import { HeaderContext, HEADER_ROW_HEIGHT } from './headers/header'
import Headers from './headers/headers'
import InnerTableElement from './inner-table-element'
import CellPlacementMemo from './cells/cell-placement'
import { DataTablePropsT } from './types'
import { useIfChanged } from '../../../../starwhale-core/src/utils/useWhatChanged'
import { useEvent } from '@starwhale/core'
import { useWhatChanged } from '@simbathesailor/use-what-changed'
import _ from 'lodash'

const STYLE = { overflow: 'auto' }
const sum = (ns: number[]): number => ns.reduce((s, n) => s + n, 0)
function MeasureScrollbarWidth(props: { onWidthChange: (width: number) => void }) {
    const [css] = themedUseStyletron()
    const outerRef = React.useRef<HTMLDivElement>(null)
    const innerRef = React.useRef<HTMLDivElement>(null)
    React.useEffect(() => {
        if (outerRef.current && innerRef.current) {
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
            ref={outerRef}
        >
            <div ref={innerRef} />
        </div>
    )
}

export function DataTable({
    selectable = false,
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
    queryinline = false,
    rows: allRows,
    rowActions,
    rowHeight = 44,
    rowHighlightIndex: rowHighlightIndexControlled,
    selectedRowIds: $selectedRowIds = new Set(),
    sortIndex,
    sortDirection,
    textQuery = '',
    controlRef,
    useStore,
}: DataTablePropsT) {
    const [, theme] = themedUseStyletron()
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
    const [measuredWidths, setMeasuredWidths] = React.useState(new Map())
    const [resizeDeltas, setResizeDeltas] = React.useState(new Map())

    const [itemIndexs, setItemIndexs] = React.useState({
        overscanColumnStartIndex: 0,
        overscanColumnStopIndex: 0,
        overscanRowStartIndex: 0,
        overscanRowStopIndex: 0,
        visibleColumnStartIndex: 0,
        visibleColumnStopIndex: 0,
        visibleRowStartIndex: 0,
        visibleRowStopIndex: 0,
    })

    const handleItemsRendered = React.useCallback(
        _.throttle(
            ({
                overscanColumnStartIndex,
                overscanColumnStopIndex,
                overscanRowStartIndex,
                overscanRowStopIndex,
                visibleColumnStartIndex,
                visibleColumnStopIndex,
                visibleRowStartIndex,
                visibleRowStopIndex,
            }) => {
                setItemIndexs({
                    overscanColumnStartIndex,
                    overscanColumnStopIndex,
                    overscanRowStartIndex,
                    overscanRowStopIndex,
                    visibleColumnStartIndex,
                    visibleColumnStopIndex,
                    visibleRowStartIndex,
                    visibleRowStopIndex,
                })
            },
            200
        ),
        []
    )

    // React.useEffect(() => {
    //     setMeasuredWidths((prev) => {
    //         return columns.map((v) => prev.get(v.key) || 60)
    //     })
    //     setResizeDeltas((prev) => {
    //         return columns.map((v, index) => prev.get(v.key) || 0)
    //     })
    // }, [columns])

    const [scrollLeft, setScrollLeft] = React.useState(0)
    const resetAfterColumnIndex = React.useCallback(
        (columnIndex) => {
            // console.log(gridRef, columnIndex)
            if (gridRef) {
                gridRef.resetAfterColumnIndex?.(columnIndex, true)
            }
        },
        [gridRef]
    )
    const handleWidthsChange = React.useCallback(
        (nextWidths) => {
            setMeasuredWidths(new Map(nextWidths))
            resetAfterColumnIndex(itemIndexs.overscanColumnStartIndex)
        },
        [setMeasuredWidths, resetAfterColumnIndex, itemIndexs]
    )
    const handleColumnResize = React.useCallback(
        (columnIndex, delta) => {
            const column = columns[columnIndex]
            setResizeDeltas((prev) => {
                const v = prev.has(column.key) ? prev.get(column.key) : 0
                prev.set(column.key, Math.max(v + delta, 0))
                return new Map(prev)
            })
            resetAfterColumnIndex(columnIndex)
        },
        [setResizeDeltas, resetAfterColumnIndex]
    )
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
                const columnIndex = rawColumns?.findIndex((c) => c.key === filter.key) ?? -1
                const column = rawColumns?.[columnIndex]

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
        const resizedWidths = columns.map((c, i) => {
            const w = (measuredWidths.get(c.key) ?? c.minWidth) + (resizeDeltas.get(c.key) ?? 0)
            if (c.maxWidth && w > c.maxWidth) {
                return c.maxWidth
            }
            return w
        })

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

            const remainder = Math.max(gridProps.width - sum(resizedWidths) - scrollbarWidth, 0)
            const filledColumnsLen = columns.filter((c) => c.fillWidth).length
            const padding = filledColumnsLen === 0 ? 0 : Math.floor(remainder / filledColumnsLen)

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
    const isSelectable = selectable
    const isQueryInline = queryinline

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

    const handleRowMouseEnter = useEvent((nextIndex) => {
        // setColumnHighlightIndex(-1)
        if (nextIndex !== rowHighlightIndex) {
            handleRowHighlightIndexChange(nextIndex)
        }
    })

    const handleColumnHeaderMouseEnter = React.useCallback(
        (columnIndex) => {
            setColumnHighlightIndex(columnIndex)
            handleRowHighlightIndexChange(-1)
        },
        [handleRowHighlightIndexChange]
    )

    const handleColumnHeaderMouseLeave = React.useCallback(() => {
        setColumnHighlightIndex(-1)
    }, [])

    React.useEffect(() => {
        if (typeof rowHighlightIndexControlled === 'number') {
            handleRowHighlightIndexChange(rowHighlightIndexControlled)
        }
    }, [rowHighlightIndexControlled, handleRowHighlightIndexChange])

    const itemData = React.useMemo(() => {
        return {
            // columnHighlightIndex,
            // warning: this can cause performance problem, and inline edit will have wrong behaviour so use row own behaviour
            // rowHighlightIndex,
            isRowSelected,
            isQueryInline,
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
        // rowHighlightIndex,
        isRowSelected,
        isSelectable,
        isQueryInline,
        rows,
        columns,
        handleSelectOne,
        textQuery,
        normalizedWidths,
    ])

    // console.log(rowHighlightIndex, resizeDeltas)

    const columnWidth = React.useCallback(
        (index) => {
            return normalizedWidths[index]
        },
        [normalizedWidths]
    )

    const InnerElement = React.useMemo(() => {
        // @ts-ignore
        return (props, ref) => <InnerTableElement {...props} data={itemData} gridRef={gridRef} />
    }, [itemData, gridRef])

    // const $background = React.useMemo(() => {
    //     if (!gridRef) return []
    //     //
    //     const [rowStartIndex, rowStopIndex] = gridRef._getVerticalRangeToRender()
    //     return new Array(rowStopIndex - rowStartIndex + 1).fill(0).map((_, rowIndex) => {
    //         return (
    //             <div
    //                 className='table-row-background'
    //                 key={rowIndex}
    //                 style={{
    //                     ...gridRef._getItemStyle(rowIndex, 0),
    //                     width: '100%',
    //                     marginBottom: gridRef._getItemStyle(rowIndex, 0).height * -1,
    //                     backgroundColor: rowHighlightIndex === rowIndex + rowStartIndex ? '#F7F8FA' : 'transparent',
    //                 }}
    //             />
    //         )
    //     })
    // }, [gridRef, rowHighlightIndex])

    const $columnsShowed = React.useMemo(() => {
        return columns.filter(
            (c, i) => i >= itemIndexs.overscanColumnStartIndex && i <= itemIndexs.overscanColumnStopIndex
        )
    }, [columns, itemIndexs])

    // useIfChanged({
    //     setGridRef,
    //     InnerElement,
    //     columnWidth,
    //     length: columns.length,
    //     itemData,
    //     handleScroll,
    //     rowLength: rows.length,
    //     rowHeightAtIndex,
    //     handleRowMouseEnter,
    //     // columnHighlightIndex,
    //     // rowHighlightIndex,
    //     isRowSelected,
    //     isSelectable,
    //     isQueryInline,
    //     rows,
    //     columns,
    //     handleSelectOne,
    //     textQuery,
    //     normalizedWidths,
    // })

    return (
        <>
            <MeasureColumnWidths
                columns={$columnsShowed}
                rows={rows}
                isSelectable={isSelectable}
                isQueryInline={isQueryInline}
                onWidthsChange={handleWidthsChange}
            />
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
                            isQueryInline,
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
                        {/* <div
                            style={{
                                width: `${width}px`,
                                position: 'absolute',
                                top: HEADER_ROW_HEIGHT,
                                height: height - HEADER_ROW_HEIGHT,
                                marginBottom: (height - HEADER_ROW_HEIGHT) * -1,
                            }}
                        >
                            {$background}
                        </div> */}
                        <VariableSizeGrid
                            className='table-columns'
                            ref={setGridRef as any}
                            overscanRowCount={0}
                            overscanColumnCount={0}
                            innerElementType={InnerElement}
                            height={height - HEADER_ROW_HEIGHT}
                            columnWidth={columnWidth}
                            columnCount={columns.length}
                            width={width}
                            itemData={itemData}
                            onScroll={handleScroll}
                            rowCount={rows.length}
                            rowHeight={rowHeightAtIndex}
                            style={STYLE}
                            direction={theme.direction === 'rtl' ? 'rtl' : 'ltr'}
                            onItemsRendered={handleItemsRendered}
                        >
                            {CellPlacementMemo as any}
                        </VariableSizeGrid>
                    </HeaderContext.Provider>
                )}
            </AutoSizer>
        </>
    )
}
