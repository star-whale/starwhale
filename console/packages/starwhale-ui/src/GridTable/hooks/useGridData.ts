import { useDatastoreColumns } from '@starwhale/ui/GridDatastoreTable'
import { useStoreApi } from './useStore'
import React, { useMemo } from 'react'

function useGirdData() {
    const { getId, getColumns, columnTypes, columnHints, records, fillable } = useStoreApi().getState()

    const $tablePropsColumns = React.useMemo(() => {
        if (!getColumns || typeof getColumns !== 'function') return undefined

        return getColumns?.() ?? []
    }, [getColumns])

    const $columns = useDatastoreColumns({
        fillWidth: !!fillable,
        columnTypes,
        columnHints,
    })

    const $originalColumns = React.useMemo(() => {
        return $tablePropsColumns && $tablePropsColumns.length > 0 ? $tablePropsColumns : $columns
    }, [$tablePropsColumns, $columns])

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
        originalColumns: $originalColumns,
        rows,
    }
}

export { useGirdData }

export default useGirdData
