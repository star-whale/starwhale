import React from 'react'
import { RecordSchemaT, isSearchColumns } from '@starwhale/core/datastore'
import { CustomColumn } from '../../base/data-table'
import { ColumnT, RenderCellT } from '../../base/data-table/types'
import { StringCell } from '../../base/data-table/column-string'
import _ from 'lodash'

export const sortColumn = (ca: { name: string }, cb: { name: string }) => {
    if (ca.name === 'sys/id') return -1
    if (cb.name === 'sys/id') return 1
    if (ca.name?.startsWith('sys/') && cb.name?.startsWith('sys/')) {
        return ca.name.localeCompare(cb.name)
    }
    if (ca.name?.startsWith('sys/') && !cb.name?.startsWith('sys/')) {
        return -1
    }
    if (!ca.name?.startsWith('sys/') && cb.name?.startsWith('sys/')) {
        return 1
    }

    return ca.name.localeCompare(cb.name)
}

function RenderMixedCell({ value, ...props }: RenderCellT<any>['props']) {
    return (
        <StringCell {...props} lineClamp={1} value={typeof value === 'object' ? JSON.stringify(value, null) : value} />
    )
}

export function useDatastoreColumns(columnTypes?: RecordSchemaT[]): ColumnT[] {
    const columns = React.useMemo(() => {
        const columnsWithAttrs: ColumnT[] = []

        columnTypes
            ?.filter((column) => !!column)
            .filter((column) => {
                return isSearchColumns(column.name)
            })
            .sort(sortColumn)
            .forEach((column) => {
                return columnsWithAttrs.push(
                    CustomColumn({
                        columnType: column,
                        key: column.name,
                        title: column.name,
                        renderCell: RenderMixedCell as any,
                        mapDataToValue: (data: any): string => _.get(data, [column.name, 'value']),
                    })
                )
            })

        return columnsWithAttrs
    }, [columnTypes])

    return columns
}
