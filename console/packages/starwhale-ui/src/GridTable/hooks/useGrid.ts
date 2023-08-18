import useGridQueryText from './useGridQueryText'
import useGridSave from './useGridSave'
import useGridSelection from './useGridSelection'
import useGridSort from './useGridSort'
import useGirdData from './useGridData'
import useGridQuery from './useGridQuery'
import { IGridState } from '../types'
import { useStore } from './useStore'
import shallow from 'zustand/shallow'
import { useState } from 'react'
import { RecordAttr } from '@starwhale/ui/GridDatastoreTable/recordAttrModel'
import useGridCurrentView from './useGridCurrentView'

const selector = (s: IGridState) => ({
    initStore: s.initStore,
})

function useGrid() {
    const { initStore } = useStore(selector, shallow)
    const { rows, originalColumns } = useGirdData()
    const { ids, isAllRuns, columns, currentView, setCurrentView } = useGridCurrentView(originalColumns)
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
    const [preview, setPreview] = useState<{
        record?: RecordAttr
        columnKey: string
    }>({
        record: undefined,
        columnKey: '',
    })

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
        setCurrentView,
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
        // store
        initStore,
        // preview
        preview,
        onPreview: setPreview,
        onPreviewClose: () => setPreview({ record: undefined, columnKey: '' }),
    }
}

export { useGrid }

export default useGrid
