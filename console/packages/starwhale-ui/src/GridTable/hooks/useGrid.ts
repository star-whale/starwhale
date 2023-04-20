import useGridQueryText from './useGridQueryText'
import useGridSave from './useGridSave'
import useGridSelection from './useGridSelection'
import useGridSort from './useGridSort'
import React, { useMemo } from 'react'
import useGirdData from './useGridData'

function useGrid() {
    const { onSave, onSaveAs, changed } = useGridSave()
    const { sortIndex, sortDirection } = useGridSort()
    const { textQuery, setTextQuery } = useGridQueryText()
    const { onSelectMany, onSelectNone, onSelectOne, isSelectedAll, isSelectedIndeterminate, isRowSelected } =
        useGridSelection()
    const { ids, isAllRuns, columns, currentView, rows } = useGirdData()

    return {
        onSave,
        onSaveAs,
        changed,
        // sort
        sortIndex,
        sortDirection,
        // query text
        textQuery,
        setTextQuery,
        // current view
        ids,
        isAllRuns,
        currentView,
        // selection
        onSelectMany,
        onSelectNone,
        onSelectOne,
        isSelectedAll,
        isSelectedIndeterminate,
        isRowSelected,
        columns,
        rows,
    }
}

export { useGrid }

export default useGrid
