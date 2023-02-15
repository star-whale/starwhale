import React from 'react'
import { ColumnSchemaDesc, DataTypes } from '@starwhale/core/datastore'
import { BooleanColumn, NumericalColumn, StringColumn } from '../../base/data-table'
import { ColumnT, FilterTypes } from '../../base/data-table/types'

const isPrivateSys = (str: string) => str.startsWith('sys/_')

export function useDatastoreColumns(columnTypes: ColumnSchemaDesc[] = []): ColumnT[] {
    const columns = React.useMemo(() => {
        const columnsWithAttrs: ColumnT[] = []

        columnTypes
            ?.sort((ca, cb) => {
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
            })
            .forEach((column) => {
                if (isPrivateSys(column.name)) return

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
