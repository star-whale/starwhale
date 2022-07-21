// @ts-nocheck

// @flow

import * as React from 'react'

import { SORT_DIRECTIONS } from './constants'
import type { ColumnT, StatefulContainerPropsT } from './types'

const IS_DEV = true

function useDuplicateColumnTitleWarning(columns: ColumnT[]) {
    React.useEffect(() => {
        if (IS_DEV) {
            const titles = columns.reduce((set, column) => set.add(column.title), new Set())
            if (titles.size < columns.length) {
                // eslint-disable-next-line
                console.warn(
                    'BaseWeb DataTable: Column titles must be unique else will result in non-deterministic filtering.'
                )
            }
        }
    }, [columns])
}

function useSortParameters(initialSortIndex = -1, initialSortDirection = null) {
    const [sortIndex, setSortIndex] = React.useState(initialSortIndex)
    const [sortDirection, setSortDirection] = React.useState<null | string>(initialSortDirection)

    function handleSort(columnIndex: number) {
        if (columnIndex === sortIndex) {
            if (sortDirection === SORT_DIRECTIONS.DESC) {
                setSortIndex(-1)
                setSortDirection(SORT_DIRECTIONS.ASC)
            } else {
                setSortDirection(SORT_DIRECTIONS.DESC)
            }
        } else {
            setSortIndex(columnIndex)
            setSortDirection(SORT_DIRECTIONS.ASC)
        }
    }

    return [sortIndex, sortDirection, handleSort]
}

export function StatefulContainer(props: StatefulContainerPropsT) {
    useDuplicateColumnTitleWarning(props.columns)
    const [sortIndex, sortDirection, handleSort] = useSortParameters(props.initialSortIndex, props.initialSortDirection)

    const [textQuery, setTextQuery] = React.useState('')

    const { onIncludedRowsChange, onRowHighlightChange } = props
    const handleIncludedRowsChange = React.useCallback(
        (rows) => {
            onIncludedRowsChange?.(rows)
        },
        [onIncludedRowsChange]
    )

    const handleRowHighlightChange = React.useCallback(
        (rowIndex, row) => {
            onRowHighlightChange?.(rowIndex, row)
        },
        [onRowHighlightChange]
    )

    return props.children({
        onIncludedRowsChange: handleIncludedRowsChange,
        onRowHighlightChange: handleRowHighlightChange,
        onSort: handleSort,
        onTextQueryChange: setTextQuery,
        resizableColumnWidths: Boolean(props.resizableColumnWidths),
        rowHighlightIndex: props.rowHighlightIndex,
        sortIndex,
        sortDirection,
        textQuery,
    })
}
