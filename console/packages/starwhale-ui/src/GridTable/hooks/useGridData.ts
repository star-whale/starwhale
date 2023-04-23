import { useDatastoreColumns } from '@starwhale/ui/GridDatastoreTable'
import useGridCurrentView from './useGridCurrentView'
import { useStoreApi } from './useStore'
import React, { useMemo } from 'react'

function useGirdData() {
    const { getId, columns, columnTypes, records } = useStoreApi().getState()

    const $rawColumns = React.useMemo(() => columns?.getColumns?.() ?? [], [columns])

    const $columns = useDatastoreColumns(columnTypes as any)

    const {
        ids,
        isAllRuns,
        columns: columnsComputed,
        currentView,
    } = useGridCurrentView($rawColumns && $rawColumns.length > 0 ? $rawColumns : $columns)

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
        columns: columnsComputed,
        rows,
        ids,
        isAllRuns,
        currentView,
    }
}

export { useGirdData }

export default useGirdData
