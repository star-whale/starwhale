import useGridQueryText from './useGridQueryText'
import useGridSave from './useGridSave'
import useGridSelection from './useGridSelection'
import useGridSort from './useGridSort'
import useGirdData from './useGridData'
import useGridQuery from './useGridQuery'
import { IGridState } from '../types'
import { useStore } from './useStore'
import { shallow } from 'zustand/shallow'
import useGridCurrentView from './useGridCurrentView'
import useGridConfigColumns from './useGridConfigColumns'
import useGridCellPreview from './useGridCellPreview'

const selector = (s: IGridState) => ({
    initStore: s.initStore,
})

function useGrid() {
    const { initStore } = useStore(selector, shallow)
    const { rows, originalColumns } = useGirdData()
    const { ids, isAllRuns, columns, currentView } = useGridCurrentView()
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
    const { renderConfigQuery, renderConfigQueryInline } = useGridQuery()
    const { renderConfigColumns, renderStatefulConfigColumns } = useGridConfigColumns()
    const preview = useGridCellPreview(rows, columns)

    console.log('usegrid 2')

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
