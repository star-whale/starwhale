import React from 'react'
import { GridTable } from '@starwhale/ui/GridTable'
import { useDatastoreColumns } from '@starwhale/ui/GridDatastoreTable'

// @ts-ignore
export default function PanelTable({ columnTypes, data, storeKey, onChange, storeRef }) {
    const $columns = useDatastoreColumns(columnTypes)
    return (
        <GridTable
            columns={$columns}
            records={data}
            storeKey={storeKey}
            queryinline
            onChange={onChange}
            storeRef={storeRef}
        />
    )
}
