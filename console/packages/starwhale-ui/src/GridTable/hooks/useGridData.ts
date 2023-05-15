import { useDatastoreColumns } from '@starwhale/ui/GridDatastoreTable'
import useGridCurrentView from './useGridCurrentView'
import { useStoreApi } from './useStore'
import React, { useMemo } from 'react'

function useGirdData() {
    const { getId, getColumns, columnTypes, records } = useStoreApi().getState()

    const $tablePropsColumns = React.useMemo(() => {
        if (!getColumns || typeof getColumns !== 'function') return undefined

        return getColumns?.() ?? []
    }, [getColumns])

    const $columns = useDatastoreColumns(columnTypes as any)

    const $originalColumns = React.useMemo(() => {
        return $tablePropsColumns && $tablePropsColumns.length > 0 ? $tablePropsColumns : $columns
    }, [$tablePropsColumns, $columns])

    const { ids, isAllRuns, columns: columnsComputed, currentView } = useGridCurrentView($originalColumns)

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
        originalColumns: $originalColumns,
        rows,
        ids,
        isAllRuns,
        currentView,
    }
}

export { useGirdData }

export default useGirdData
