import useGridCurrentView from './useGridCurrentView'
import useGridSave from './useGridSave'
import useGridSort from './useGridSort'

function useGrid() {
    const { onSave, onSaveAs, changed } = useGridSave()
    const { sortIndex, sortDirection } = useGridSort()
    const { ids, isAllRuns, columns, currentView } = useGridCurrentView()

    return { onSave, onSaveAs, changed, sortIndex, sortDirection }
}

export { useGrid }

export default useGrid
