// @ts-nocheck
import * as React from 'react'
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

export function StatefulContainer(props: StatefulContainerPropsT) {
    useDuplicateColumnTitleWarning(props.columns)

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
        onTextQueryChange: setTextQuery,
        resizableColumnWidths: Boolean(props.resizableColumnWidths),
        rowHighlightIndex: props.rowHighlightIndex,
        textQuery,
    })
}
