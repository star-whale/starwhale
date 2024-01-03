import { useDatastoreColumns } from '@starwhale/ui/GridDatastoreTable'
import { useStoreApi } from './useStore'
import React, { useMemo } from 'react'

function useGirdData() {
    const { getId, originalColumns, columns, records, $columns } = useStoreApi().getState()

    const rows = useMemo(() => {
        return (
            records?.map((raw, index) => {
                // console.log(raw, getId)
                return {
                    id: getId?.(raw) ?? index.toFixed(),
                    data: raw,
                }
            }) ?? []
        )
    }, [records, getId])

    return {
        originalColumns: originalColumns ?? columns,
        rows,
    }
}

export { useGirdData }

export default useGirdData
