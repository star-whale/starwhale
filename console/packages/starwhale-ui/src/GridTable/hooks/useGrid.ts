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
import { GirdRecord } from '@starwhale/ui/GridDatastoreTable/recordAttrModel'

const selector = (s: IGridState) => ({
    initStore: s.initStore,
})

function useGrid() {
    const { initStore } = useStore(selector, shallow)
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
    const { ids, isAllRuns, columns, currentView, rows, originalColumns } = useGirdData()
    const { renderConfigQuery } = useGridQuery()
    const [preview, setPreview] = useState<{
        record?: GirdRecord
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
