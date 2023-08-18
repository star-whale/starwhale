import React, { useCallback } from 'react'
import { Tooltip, PLACEMENT } from 'baseui/tooltip'
import cn from 'classnames'
import Header, { HeaderContext, HEADER_ROW_HEIGHT } from './header'
import type { ColumnT, SortDirectionsT } from '../types'
import { LocaleContext } from 'baseui/locale'
import { themedUseStyletron } from '../../../theme/styletron'
import { useStore, useStoreApi } from '@starwhale/ui/GridTable/hooks/useStore'
import { IGridState } from '@starwhale/ui/GridTable/types'
import useGridQuery from '@starwhale/ui/GridTable/hooks/useGridQuery'
import useGrid from '@starwhale/ui/GridTable/hooks/useGrid'

const sum = (ns: number[]): number => ns.reduce((s, n) => s + n, 0)

const selector = (s: IGridState) => ({
    queryinline: s.queryinline,
    compare: s.compare,
})

export default function Headers({ width }: { width: number }) {
    const [css, theme] = themedUseStyletron()
    const locale = React.useContext(LocaleContext)
    const ctx = React.useContext(HeaderContext)
    const [resizeIndex, setResizeIndex] = React.useState(-1)
    const { queryinline } = useStore(selector)
    const { onNoSelect, onCompareUpdate, onCurrentViewColumnsPin, onCurrentViewSort, compare } =
        useStoreApi().getState()
    const { columns, selectedRowIds } = useGrid()

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
        return $columns.filter((v) => v.pin !== 'LEFT').map(headerRender)
    }, [$columns, headerRender])

    return (
        <div
            className={cn(
                'table-headers-wrapper',
                css({
                    position: 'sticky',
                    top: 0,
                    left: 0,
                    display: 'flex',
                    zIndex: 2,
                    backgroundColor: '#F3F5F9',
                    overflow: 'hidden',
                })
            )}
            style={{
                width,
            }}
        >
            <div
                className='table-headers-pinned'
                style={{
                    position: 'sticky',
                    left: 0,
                    zIndex: 50,
                    borderLeftWidth: '0',
                    overflow: 'visible',
                    breakInside: 'avoid',
                    display: 'flex',
                    height: HEADER_ROW_HEIGHT,
                }}
            >
                {headersLeft}
            </div>

            {headers.length > 0 && (
                <>
                    <div
                        className='table-headers'
                        style={{
                            width: '100%',
                            position: 'absolute',
                            left: 0,
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
