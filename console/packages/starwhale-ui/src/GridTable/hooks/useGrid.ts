import useGridQueryText from './useGridQueryText'
import useGridSave from './useGridSave'
import useGridSelection from './useGridSelection'
import useGridQuery from './useGridQuery'
import { IGridState } from '../types'
import { useStore } from './useStore'
import { shallow } from 'zustand/shallow'
import useGridCurrentView from './useGridCurrentView'
import useGridConfigColumns from './useGridConfigColumns'
import useGridCellPreview from './useGridCellPreview'

const selector = (s: IGridState) => ({
    initStore: s.initStore,
    originalColumns: s.originalColumns,
    rows: s.compute?.rows ?? [],
    sortIndex: s.compute?.sortIndex,
    sortDirection: s.currentView.sortDirection,
    columns: s.compute?.columns ?? [],
})

function useGrid() {
    const { initStore, originalColumns, rows, columns, sortIndex, sortDirection } = useStore(selector, shallow)
    const { ids, isAllRuns, currentView } = useGridCurrentView()
    const { onSave, onSaveAs, changed } = useGridSave()
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
    const { renderConfigQuery, renderConfigQueryInline } = useGridQuery()
    const { renderConfigColumns, renderStatefulConfigColumns } = useGridConfigColumns()
    const preview = useGridCellPreview(rows, columns)

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
        originalColumns,
        rows,
        // query
        renderConfigQuery,
        renderConfigQueryInline,
        // columns
        renderConfigColumns,
        renderStatefulConfigColumns,
        // store
        initStore,
        preview,
    }
}

export { useGrid }

export default useGrid
