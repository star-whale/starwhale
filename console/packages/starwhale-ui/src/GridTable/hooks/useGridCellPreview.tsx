import { useEventCallback } from '@starwhale/core'
import { RecordAttr } from '@starwhale/ui/GridDatastoreTable/recordAttrModel'
import { ColumnT } from '@starwhale/ui/base/data-table/types'
import React, { useState } from 'react'

function useGridCellPreview(rows, columns: ColumnT[]) {
    const [preview, setPreview] = useState<{
        record?: RecordAttr
        columnKey: string
        rowIndex?: number
        columnIndex?: number
    }>({
        record: undefined,
        columnKey: '',
        rowIndex: 0,
        columnIndex: 0,
    })

    const { rowIndex = 0, columnIndex = 0, columnKey } = preview ?? {}

    const { prev, next } = React.useMemo(() => {
        const column = columns?.[columnIndex]
        const { mapDataToValue } = column || {}
        if (!columnKey)
            return {
                prev: undefined,
                next: undefined,
            }
        return {
            prev: rows?.[rowIndex - 1] ? mapDataToValue?.(rows?.[rowIndex - 1]?.data || {}) : undefined,
            next: rows?.[rowIndex + 1] ? mapDataToValue?.(rows?.[rowIndex + 1]?.data || {}) : undefined,
        }
    }, [rows, columns, rowIndex, columnIndex, columnKey])

    const onPreviewNext = useEventCallback(() => {
        if (next) {
            setPreview({
                record: next,
                columnKey,
                rowIndex: rowIndex + 1,
                columnIndex,
            })
        }
    })

    const onPreviewPrev = useEventCallback(() => {
        if (prev) {
            setPreview({
                record: prev,
                columnKey,
                rowIndex: rowIndex - 1,
                columnIndex,
            })
        }
    })

    return {
        // preview
        prev,
        next,
        current: preview?.record,
        previewKey: preview?.columnKey,
        onPreview: setPreview,
        onPreviewNext,
        onPreviewPrev,
        onPreviewClose: useEventCallback(() =>
            setPreview({ record: undefined, columnKey: '', rowIndex: 0, columnIndex: 0 })
        ),
    }
}
export { useGridCellPreview }
export default useGridCellPreview
