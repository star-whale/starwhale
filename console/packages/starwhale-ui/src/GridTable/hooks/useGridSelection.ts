import React from 'react'
import { ITableState } from '../store'
import { shallow } from 'zustand/shallow'
import { useStore } from './useStore'

const selector = (state: ITableState) => ({
    rowSelectedIds: state.rowSelectedIds,
    onSelectMany: state.onSelectMany,
    onSelectNone: state.onSelectNone,
    onSelectOne: state.onSelectOne,
    rows: state.compute?.rows,
})

function useGridSelection() {
    const { rows, rowSelectedIds, onSelectMany, onSelectNone, onSelectOne } = useStore(selector, shallow)

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
        onSelectMany?.(
            rows.map((row) => row.id),
            rows.map((row) => row.data)
        )
    }, [rows, onSelectMany])

    const handleSelectNone = React.useCallback(() => {
        onSelectNone?.()
    }, [onSelectNone])

    const handleSelectOne = React.useCallback(
        (row) => {
            onSelectOne?.(row.id, row.data)
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
        selectedRowIds,
    }
}

export { useGridSelection }

export default useGridSelection
