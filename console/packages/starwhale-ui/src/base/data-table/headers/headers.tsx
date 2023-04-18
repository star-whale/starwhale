import React, { useCallback } from 'react'
import { Tooltip, PLACEMENT } from 'baseui/tooltip'
import cn from 'classnames'
import Header, { HeaderContext, HEADER_ROW_HEIGHT } from './header'
import type { ColumnT, SortDirectionsT } from '../types'
import { LocaleContext } from 'baseui/locale'
import { themedUseStyletron } from '../../../theme/styletron'
import { useConfigQuery } from '../config-query'
import { useWhatChanged } from '@simbathesailor/use-what-changed'
import { useStore, useStoreApi } from '@starwhale/ui/GridTable/hooks/useStore'
import { ITableState } from '@starwhale/ui/GridTable/store'

const sum = (ns: number[]): number => ns.reduce((s, n) => s + n, 0)

const selector = (s: ITableState) => ({
    columns: s.columns,
    isQueryInline: s.isQueryInline,
})

export default function Headers({ width }: { width: number }) {
    // @FIXME css as dep will cause rerender ?
    const [css, theme] = themedUseStyletron()
    const locale = React.useContext(LocaleContext)
    const ctx = React.useContext(HeaderContext)
    const [resizeIndex, setResizeIndex] = React.useState(-1)
    const { columns, isQueryInline } = useStore(selector)
    const store = useStoreApi().getState()

    const $columns = React.useMemo(
        () =>
            columns.map((v, index) => ({
                ...v,
                index,
            })),
        [columns]
    )

    const { renderConfigQueryInline } = useConfigQuery({
        columns,
        queryable: isQueryInline,
    })

    const headerRender = useCallback(
        (column: ColumnT & { index: number }, index) => {
            const columnIndex = column.index

            const handleNoSelect = () => {
                store.onNoSelect(column.key as string)
            }

            const handleFocus = () => {
                store.onCompareUpdate({
                    comparePinnedKey: column.key as string,
                })
            }
            const handlePin = (index: number, bool: boolean) => {
                store.onCurrentViewColumnsPin(column.key as string, bool)
            }

            // @ts-ignore
            const handleSort = (index: number, direction: SortDirectionsT) => {
                store.onCurrentViewSort(column.key as string, direction)
            }

            const isFoucs = column.key === store.compare?.comparePinnedKey
            const isPin = !!column.pin

            return (
                <Tooltip key={columnIndex} placement={PLACEMENT.bottomLeft}>
                    <div
                        style={{
                            width: ctx.widths[columnIndex],
                            ...theme.borders.border200,
                            backgroundColor: theme.colors.backgroundPrimary,
                            borderTop: 'none',
                            borderBottom: 'none',
                            borderRight: 'none',
                            borderLeft: 'none',
                            boxSizing: 'border-box',
                            display: 'flex',
                            height: `${HEADER_ROW_HEIGHT}px`,
                        }}
                    >
                        <Header
                            columnTitle={column.title}
                            hoverIndex={ctx.columnHighlightIndex}
                            index={columnIndex}
                            isSortable={column.sortable}
                            isSelectable={ctx.isSelectable}
                            isSelectedAll={ctx.isSelectedAll}
                            isSelectedIndeterminate={ctx.isSelectedIndeterminate}
                            isQueryInline={isQueryInline}
                            querySlot={renderConfigQueryInline({ width })}
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
                            resizeIndex={resizeIndex}
                            resizeMinWidth={ctx.measuredWidths.get(column.key)}
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
            store,
            setResizeIndex,
            resizeIndex,
            locale,
            theme,
            ctx.columnHighlightIndex,
            ctx.isSelectable,
            ctx.isSelectedAll,
            ctx.isSelectedIndeterminate,
            isQueryInline,
            ctx.measuredWidths,
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
            renderConfigQueryInline,
        ]
    )

    const headersLeft = React.useMemo(() => {
        return $columns.filter((v) => v.pin === 'LEFT').map(headerRender)
    }, [$columns, headerRender])

    const headersLeftWidth = React.useMemo(() => {
        return sum($columns.map((v, index) => (v.pin === 'LEFT' ? ctx.widths[index] : 0)))
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
