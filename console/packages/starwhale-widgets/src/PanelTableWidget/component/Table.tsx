import React from 'react'
import { GridTable, useDatastoreColumns } from '@starwhale/ui/GridTable'

// @ts-ignore
export default function PanelTable({ columnTypes, data, storeKey }) {
    const $columns = useDatastoreColumns(columnTypes)
    return <GridTable columns={$columns} data={data} storeKey={storeKey} />
}
