import { useDatastoreColumns } from '@starwhale/ui/GridDatastoreTable'
import useGridCurrentView from './useGridCurrentView'
import { useStore, useStoreApi } from './useStore'
import { useMemo } from 'react'
import { IGridState } from '../types'
import shallow from 'zustand/shallow'

const selector = (state: IGridState) => ({
    columnTypes: state.columnTypes,
    records: state.records,
})

function useGirdData() {
    const { columnTypes, records } = useStore(selector, shallow)
    const { getId } = useStoreApi().getState()

    const $columns = useDatastoreColumns(columnTypes as any)
    const { ids, isAllRuns, columns, currentView } = useGridCurrentView($columns)
    const rows = useMemo(
        () =>
            records?.map((raw, index) => {
                // console.log(raw, getId)
                return {
                    id: getId?.(raw) ?? index.toFixed(),
                    data: raw,
                }
            }) ?? [],
        [records, getId]
    )
    return {
        columns,
        rows,
        ids,
        isAllRuns,
        currentView,
    }
}

export { useGirdData }

export default useGirdData
