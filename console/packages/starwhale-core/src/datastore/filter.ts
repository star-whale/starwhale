import { OPERATOR, DataTypes } from './constants'
import { ColumnDesc, ColumnSchemaDesc, TableQueryFilterDesc, TableQueryOperandDesc } from './schemas/datastore'
import { isBasicType, isSearchColumns } from './utils'

export type ColumnSchemaFlatternT = {
    name: string
    type: string
    path: string
    label: string
} & ColumnDesc

export class ColumnFilterModel {
    columnTypes: ColumnSchemaDesc[]

    constructor(columnTypes: ColumnSchemaDesc[] = []) {
        this.columnTypes = columnTypes
    }

    /**
    [(
        { name: 'data_size', type: 'INT64', path: 'data_size' },
        { name: 'label', type: 'INT64', path: 'annotations.label' }
    )]
     */
    getSearchColumns(): ColumnSchemaFlatternT[] {
        const columns = this.columnTypes.filter((column) => isSearchColumns(column.name))
        const arr: ColumnSchemaFlatternT[] = []
        columns.forEach((column) => {
            // if (column.type === 'OBJECT') {
            //     const { attributes = [] } = column
            //     attributes.forEach((value) => {
            //         arr.push({
            //             name: value.name,
            //             type: value.type,
            //             path: `${column.name}.${value.name}`,
            //             label: `${column.name}/${value.name}`,
            //         })
            //     })
            // } else
            if (isBasicType(column.type)) {
                arr.push({
                    ...column,
                    path: column.name,
                    label: column.name,
                })
            }
        })
        return arr
    }

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
    addQuery(columnName: string, value: string, operator: OPERATOR, type: DataTypes): TableQueryFilterDesc | undefined {
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
        let operands = [{ columnName }]
        if (!queryType) return
        operands.push({ [queryType]: value } as any)

        return {
            operator: operator as string,
            operands,
        }
    }

    toQuery(items: any): TableQueryFilterDesc {
        const fields = this.getSearchColumns()
        const filters = items
            .filter((item: any) => item?.value && item?.op && item?.property)
            .map((item: any) => {
                const field = fields.find((f) => f.name === item.property) as any
                return this.addQuery(field?.name, item.value, item.op, field?.type as DataTypes)
            })
        if (filters.length === 1) return filters[0]

        return {
            operator: 'AND',
            operands: filters,
        }
    }
}
