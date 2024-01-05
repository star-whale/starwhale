import React, { useCallback } from 'react'
import { Tooltip, PLACEMENT } from 'baseui/tooltip'
import Header, { HeaderContext, HEADER_ROW_HEIGHT } from './header'
import type { ColumnT, SortDirectionsT } from '../types'
import { themedUseStyletron } from '../../../theme/styletron'
import { useStore, useStoreApi } from '../../../GridTable/hooks/useStore'
import { IGridState } from '../../../GridTable/types'
import HeaderBar from './header-bar'
import useTranslation from '@/hooks/useTranslation'

const sum = (ns: number[]): number => ns.reduce((s, n) => s + n, 0)

const selector = (s: IGridState) => ({
    queryinline: s.queryinline,
    compare: s.compare,
    selectedRowIds: s.rowSelectedIds,
})

export default function Headers({ width }: { width: number }) {
    const [, theme] = themedUseStyletron()
    const ctx = React.useContext(HeaderContext)
    const [resizeIndex, setResizeIndex] = React.useState(-1)
    const { queryinline, selectedRowIds } = useStore(selector)
    const { onNoSelect, onCompareUpdate, onCurrentViewColumnsPin, onCurrentViewSort, compare } =
        useStoreApi().getState()
    const { columns } = ctx

    const [t] = useTranslation()

    const $columns = React.useMemo(
        () =>
            columns.map((v, index) => ({
                ...v,
                index,
            })),
        [columns]
    )

    const headerRender = useCallback(
        (column: ColumnT & { index: number }, index) => {
            const columnIndex = column.index

            const handleNoSelect = () => {
                onNoSelect(column.key as string)
            }

            const handleFocus = () => {
                onCompareUpdate({
                    comparePinnedKey: column.key as string,
                })
            }
            const handlePin = (index: number, bool: boolean) => {
                onCurrentViewColumnsPin(column.key as string, bool)
            }

            // @ts-ignore
            const handleSort = (index: number, direction: SortDirectionsT) => {
                onCurrentViewSort(column.key as string, direction)
            }

            const isFoucs = column.key === compare?.comparePinnedKey
            const isPin = !!column.pin

            if (
                (columnIndex < ctx.overscanColumnStartIndex || columnIndex > ctx.overscanColumnStopIndex) &&
                columnIndex !== 0 &&
                !column.pin
            ) {
                return <div key={column.key} style={{ width: ctx.widths.get(column.key) }} />
            }

            return (
                <Tooltip key={columnIndex} placement={PLACEMENT.bottomLeft}>
                    <div
                        data-type='header'
                        style={{
                            ...theme.borders.border200,
                            backgroundColor: theme.colors.backgroundPrimary,
                            borderTop: 'none',
                            borderBottom: 'none',
                            borderRight: 'none',
                            borderLeft: 'none',
                            boxSizing: 'border-box',
                            display: 'flex',
                            height: `${HEADER_ROW_HEIGHT}px`,
                            width: `${ctx.widths.get(column.key)}px`,
                        }}
                    >
                        <Header
                            width={ctx.widths.get(column.key)}
                            columnTitle={column.title}
                            hoverIndex={ctx.columnHighlightIndex}
                            index={columnIndex}
                            isSortable={column.sortable}
                            isSelectable={ctx.isSelectable}
                            isSelectedAll={ctx.isSelectedAll}
                            isSelectedIndeterminate={ctx.isSelectedIndeterminate}
                            selectedRowIds={selectedRowIds}
                            isQueryInline={queryinline}
                            wrapperWidth={width}
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
                            removable={ctx.removable}
                            resizeIndex={resizeIndex}
                            resizeMinWidth={column.minWidth || 0}
                            resizeMaxWidth={column.maxWidth || Infinity}
                            sortIndex={ctx.sortIndex}
                            sortDirection={ctx.sortDirection}
                            tableHeight={ctx.tableHeight}
                        />
                    </div>
                </Tooltip>
            )
        },
        [
            ctx.overscanColumnStartIndex,
            ctx.overscanColumnStopIndex,
            selectedRowIds,
            ctx.removable,
            onNoSelect,
            onCompareUpdate,
            onCurrentViewColumnsPin,
            onCurrentViewSort,
            compare,
            setResizeIndex,
            resizeIndex,
            theme,
            ctx.columnHighlightIndex,
            ctx.isSelectable,
            ctx.isSelectedAll,
            ctx.isSelectedIndeterminate,
            queryinline,
            ctx.onMouseEnter,
            ctx.onMouseLeave,
            ctx.onResize,
            ctx.onSelectMany,
            ctx.onSelectNone,
            ctx.resizableColumnWidths,
            ctx.sortDirection,
            ctx.sortIndex,
            ctx.tableHeight,
            ctx.widths,
            ctx.compareable,
            width,
        ]
    )

    const headersLeft = React.useMemo(() => {
        return $columns.filter((v) => v.pin === 'LEFT').map(headerRender)
    }, [$columns, headerRender])

    const headersLeftWidth = React.useMemo(() => {
        return sum($columns.map((v) => (v.pin === 'LEFT' ? ctx.widths.get(v.key) : 0)))
    }, [$columns, ctx.widths])

    const headers = React.useMemo(() => {
        return $columns.filter((v) => v.pin !== 'LEFT' && v.pin !== 'RIGHT').map(headerRender)
    }, [$columns, headerRender])

    const headersRightCount = React.useMemo(() => {
        return $columns.filter((v) => v.pin === 'RIGHT')?.length || 0
    }, [$columns])

    const headerRightWidth = React.useMemo(() => {
        return sum($columns.map((v) => (v.pin === 'RIGHT' ? ctx.widths.get(v.key) : 0)))
    }, [$columns, ctx.widths])

    return (
        <div
            className='table-headers-wrapper sticky top-0 left-0 flex z-2 overflow-hidden bg-[#F3F5F9]'
            style={{
                width,
                height: HEADER_ROW_HEIGHT,
            }}
        >
            <div
                className='table-headers-bar absolute left-13px z-51 border-l-0 overflow-visible break-inside-avoid flex '
                style={{
                    height: HEADER_ROW_HEIGHT,
                }}
            >
                {!ctx.isSelectable && <HeaderBar wrapperWidth={width} />}
            </div>
            <div className='table-headers-pinned sticky left-0 z-50 overflow-visible flex h-0 w-full border-l-0'>
                {headersLeft.length > 0 && (
                    <div
                        className='flex overflow-hidden'
                        style={{
                            borderRight: '1px solid #CFD7E6',
                            height: HEADER_ROW_HEIGHT,
                            width: headersLeftWidth,
                        }}
                    >
                        {headersLeft}
                    </div>
                )}
                {headersRightCount > 0 && (
                    <div
                        className='flex flex-start items-center ml-auto pl-12px bg-[#F3F5F9] font-bold text-14px'
                        style={{
                            width: headerRightWidth + ctx.scrollbarWidth - 1,
                            height: HEADER_ROW_HEIGHT,
                            borderLeft: '1px solid #CFD7E6',
                        }}
                    >
                        {t('Action')}
                    </div>
                )}
            </div>

            {headers.length > 0 && (
                <>
                    <div
                        className='table-headers absolute left-0 w-full flex'
                        style={{
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
