import React from 'react'
import { ColumnSchemaDesc, DataTypes, useDatastore } from '@starwhale/core/datastore'
import type { RecordSchemaT } from '@starwhale/core/datastore'
import { BooleanColumn, CustomColumn, NumericalColumn, StringColumn } from '../../base/data-table'
import { ColumnT, FilterTypes, RenderCellT } from '../../base/data-table/types'
import { StringCell } from '@starwhale/ui/base/data-table/column-string'

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
    return <StringCell {...props} lineClamp={1} value={value.toString()} />
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
                            columnType: column,
                            key: column.name,
                            title: column.name,
                            renderCell: RenderMixedCell as any,
                            mapDataToValue: (data: any): string => data?.[column.name],
                        })
                    )
                }

                switch (column.type) {
                    default:
                        break
                    case DataTypes.BOOL:
                        columnsWithAttrs.push(
                            BooleanColumn({
                                columnType: column,
                                key: column.name,
                                title: column.name,
                                filterType: FilterTypes.enum,
                                mapDataToValue: (data: any): boolean => data?.[column.name],
                            })
                        )
                        break
                    case DataTypes.INT8:
                    case DataTypes.INT16:
                    case DataTypes.INT32:
                    case DataTypes.INT64:
                    case DataTypes.FLOAT16:
                    case DataTypes.FLOAT32:
                    case DataTypes.FLOAT64:
                        columnsWithAttrs.push(
                            NumericalColumn({
                                columnType: column,
                                key: column.name,
                                title: column.name,
                                format: (v) => (typeof v === 'string' ? v : v?.toFixed(10)),
                                mapDataToValue: (data: any): number => data?.[column.name] ?? '',
                            })
                        )
                        break
                    case 'STRING':
                        columnsWithAttrs.push(
                            StringColumn({
                                columnType: column,
                                key: column.name,
                                title: column.name,
                                mapDataToValue: (data: any): string => data?.[column.name] ?? '-',
                            })
                        )
                        break
                }
            })

        return columnsWithAttrs
    }, [columnTypes])

    return columns
}
