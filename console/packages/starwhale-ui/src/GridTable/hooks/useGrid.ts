import useGridCurrentView from './useGridCurrentView'
import useGridQueryText from './useGridQueryText'
import useGridSave from './useGridSave'
import useGridSelection from './useGridSelection'
import useGridSort from './useGridSort'

function useGrid() {
    const { onSave, onSaveAs, changed } = useGridSave()
    const { sortIndex, sortDirection } = useGridSort()
    const { textQuery, setTextQuery } = useGridQueryText()
    const { ids, isAllRuns, columns, currentView } = useGridCurrentView()
    const { onSelectMany, onSelectNone, onSelectOne, isSelectedAll, isSelectedIndeterminate, isRowSelected } =
        useGridSelection()

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
        columns,
        currentView,
        // selection
        onSelectMany,
        onSelectNone,
        onSelectOne,
        isSelectedAll,
        isSelectedIndeterminate,
        isRowSelected,
    }
}

export { useGrid }

export default useGrid
