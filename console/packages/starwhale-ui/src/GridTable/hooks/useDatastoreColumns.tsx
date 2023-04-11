import React from 'react'
import { ColumnSchemaDesc, DataTypes, useDatastore } from '@starwhale/core/datastore'
import type { RecordSchemaT } from '@starwhale/core/datastore'
import { BooleanColumn, CustomColumn, NumericalColumn, StringColumn } from '../../base/data-table'
import { ColumnT, FilterTypes, RenderCellT } from '../../base/data-table/types'
import { StringCell } from '@starwhale/ui/base/data-table/column-string'
import _ from 'lodash'

const isPrivateSys = (str: string = '') => str.startsWith('sys/_')
const isPrivate = (str: string = '') => str.startsWith('_')
const sortSys = (ca: RecordSchemaT, cb: RecordSchemaT) => {
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

    return 1
}

function RenderMixedCell({ value, ...props }: RenderCellT<any>['props']) {
    return <StringCell {...props} lineClamp={1} value={value && value.toString()} />
}

export function useDatastoreColumns(columnTypes?: RecordSchemaT[]): ColumnT[] {
    const columns = React.useMemo(() => {
        const columnsWithAttrs: ColumnT[] = []

        columnTypes
            ?.filter((column) => !!column)
            .filter((column) => !isPrivateSys(column.name) || !isPrivate(column.name))
            .sort(sortSys)
            .forEach((column) => {
                if (column.mixed) {
                    return columnsWithAttrs.push(
                        CustomColumn({
                            key: column.name,
                            title: column.name,
                            renderCell: RenderMixedCell as any,
                            mapDataToValue: (data: any): string => _.get(data, [column.name, 'value']),
                        })
                    )
                }
            })

        return columnsWithAttrs
    }, [columnTypes])

    return columns
}
