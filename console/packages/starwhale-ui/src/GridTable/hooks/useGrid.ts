import useGridQueryText from './useGridQueryText'
import useGridSave from './useGridSave'
import useGridSelection from './useGridSelection'
import useGridSort from './useGridSort'
import useGirdData from './useGridData'
import useGridQuery from './useGridQuery'

function useGrid() {
    const { onSave, onSaveAs, changed } = useGridSave()
    const { sortIndex, sortDirection } = useGridSort()
    const { textQuery, setTextQuery } = useGridQueryText()
    const {
        selectedRowIds,
        onSelectMany,
        onSelectNone,
        onSelectOne,
        isSelectedAll,
        isSelectedIndeterminate,
        isRowSelected,
    } = useGridSelection()
    const { ids, isAllRuns, columns, currentView, rows } = useGirdData()
    const { renderConfigQuery } = useGridQuery({ columns })

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
        selectedRowIds,
        onSelectMany,
        onSelectNone,
        onSelectOne,
        isSelectedAll,
        isSelectedIndeterminate,
        isRowSelected,
        // data
        columns,
        rows,
        // query
        renderConfigQuery,
    }
}

export { useGrid }

export default useGrid
