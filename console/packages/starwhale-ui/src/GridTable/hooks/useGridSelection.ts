import React from 'react'
import { useStoreApi } from './useStore'
import { ITableState } from '../store'

const selector = (state: ITableState) => ({
    rowSelectedIds: state.rowSelectedIds,
    rows: state.rows,
    onSelectMany: state.onSelectMany,
    onSelectNone: state.onSelectNone,
    onSelectOne: state.onSelectOne,
})

function useGridSelection() {
    const { rowSelectedIds, rows, onSelectMany, onSelectNone, onSelectOne } = useStoreApi(selector).getState()

    const selectedRowIds = React.useMemo(() => {
        if (rowSelectedIds) {
            return new Set(rowSelectedIds)
        }
        return new Set()
    }, [rowSelectedIds])

    const isSelectedAll = React.useMemo(() => {
        if (!rows) return false
        if (!selectedRowIds) {
            return false
        }
        return !!rows.length && selectedRowIds.size >= rows.length
    }, [selectedRowIds, rows])
    const isSelectedIndeterminate = React.useMemo(() => {
        if (!rows) return false
        if (!selectedRowIds) {
            return false
        }
        return !!selectedRowIds.size && selectedRowIds.size < rows.length
    }, [selectedRowIds, rows])
    const isRowSelected = React.useCallback(
        (row) => {
            if (selectedRowIds) {
                return selectedRowIds.has(row.id)
            }
            return false
        },
        [selectedRowIds]
    )
    const handleSelectMany = React.useCallback(() => {
        if (onSelectMany) {
            onSelectMany(rows.map((row) => row.id))
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

    return {
        isSelectedAll,
        isSelectedIndeterminate,
        isRowSelected,
        onSelectMany: handleSelectMany,
        onSelectNone: handleSelectNone,
        onSelectOne: handleSelectOne,
    }
}

export { useGridSelection }

export default useGridSelection
