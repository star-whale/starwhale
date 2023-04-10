import React from 'react'
import { OPERATOR, DataTypes } from './constants'
import { ColumnDesc, ColumnSchemaDesc, TableQueryFilterDesc, TableQueryOperandDesc } from './schemas/datastore'
import { isBasicType, isSearchColumns } from './utils'
import { RecordSchemaT } from '@starwhale/core/datastore'

export type RecordFilterSchemaT = {
    name: string
    type: string
    path: string
    label: string
} & ColumnDesc

export function useDatastoreFilter(columnTypes?: RecordSchemaT[], options: { mixed: boolean } = { mixed: true }) {
    const searchColumns = React.useMemo(() => {
        const arr: RecordFilterSchemaT[] = []
        const columns = columnTypes.filter((column) => isSearchColumns(column.name))
        columns.forEach((column) => {
            if (isBasicType(column.type)) {
                arr.push({
                    ...column,
                    path: column.name,
                    label: column.name,
                })
            }
        })

        return columns
    }, [])

    /**
        "operator": "AND",
        "operands": [
            {
                "operator": "EQUAL",
                "operands": [
                    {
                        "columnName": "a"
                    },
                    {
                        "intValue": "0"
                    }
                ]
            },
            {
                "operator": "GREATER",
                "operands": [
                    {
                        "columnName": "b"
                    },
                    {
                        "intValue": "0"
                    }
                ]
            }
        ]
     */
    const getQuery = React.useCallback(
        (columnName: string, value: string, operator: OPERATOR, type: DataTypes): TableQueryOperandDesc | undefined => {
            let queryType
            switch (type) {
                case DataTypes.FLOAT16:
                case DataTypes.FLOAT32:
                case DataTypes.FLOAT64:
                    queryType = 'floatValue'
                    break
                case DataTypes.INT64:
                    queryType = 'intValue'
                    break
                case DataTypes.STRING:
                    queryType = 'stringValue'
                    break
                case DataTypes.BOOL:
                    queryType = 'boolValue'
                    break
                case DataTypes.BYTES:
                    queryType = 'bytesValue'
                    break
                default:
                    break
            }
            if (options.mixed) {
                queryType = 'stringValue'
            }
            const operands = [{ columnName }]
            if (!queryType) return undefined
            operands.push({ [queryType]: value } as any)

            return {
                filter: {
                    operator: operator as string,
                    operands,
                },
            }
        },
        []
    )

    const toQuery = React.useCallback(
        (items: any): TableQueryFilterDesc | undefined => {
            const filters = items
                .map((item: any) => ({
                    ...item,
                    type: DataTypes.STRING,
                }))
                .filter((item: any) => item?.value && item?.op && item?.property)
                .map((item: any) => {
                    return getQuery(item?.property, item.value, item.op, item?.type as DataTypes)
                })

            if (filters.length === 1) return filters[0]?.filter

            if (filters.length === 0) return undefined

            return {
                operator: 'AND',
                operands: filters,
            }
        },
        [searchColumns, getQuery]
    )

    return {
        searchColumns,
        getQuery,
        toQuery,
    }
}
