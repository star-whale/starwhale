import { DataTypes, OPERATOR } from '../constants'
import { TableQueryFilterDesc } from './datastore'

type TableQueryOperandT = {
    columnName?: string
    value?: any
    type?: DataTypes
    operator?: OPERATOR
}

class TableQueryFilter {
    public operator: string

    public operands: TableQueryOperandT[]

    constructor(operator, operands) {
        this.operator = operator
        this.operands = Array.isArray(operands) ? operands : [operands]
    }

    static convertOperandType(type, value) {
        if (value instanceof TableQueryFilter) return value

        // with type
        switch (type) {
            case DataTypes.FLOAT16:
            case DataTypes.FLOAT32:
            case DataTypes.FLOAT64:
                return {
                    floatValue: value,
                }
            case DataTypes.INT64:
                return {
                    intValue: value,
                }
            case DataTypes.STRING:
                return {
                    stringValue: value,
                }
            case DataTypes.BOOL:
                return {
                    boolValue: value,
                }
            case DataTypes.BYTES:
                return {
                    bytesValue: value,
                }
            default:
                return {
                    stringValue: value,
                }
        }
    }

    static convertOperand(type, value, columnName) {
        if (value instanceof TableQueryFilter) return value
        if (Array.isArray(value)) {
            if (value.length > 1) {
                return value.map(
                    (v) =>
                        new TableQueryFilter(OPERATOR.EQUAL, [
                            { columnName },
                            TableQueryFilter.convertOperandType(type, v),
                        ])
                )
            }
            return [{ columnName }, TableQueryFilter.convertOperandType(type, value[0])]
        }
        return [{ columnName }, TableQueryFilter.convertOperandType(type, value)]
    }

    static fromUI(operand: TableQueryOperandT) {
        const { value, type, columnName, operator } = operand
        if (!value) return undefined

        switch (operator) {
            case OPERATOR.NOT_IN: {
                const v = Array.isArray(value) ? value : String(value as string).split(',')
                const operands = TableQueryFilter.convertOperand(type, v, columnName)
                if (v.length === 1) {
                    return new TableQueryFilter(OPERATOR.NOT, new TableQueryFilter(OPERATOR.EQUAL, operands))
                }
                return new TableQueryFilter(OPERATOR.NOT, new TableQueryFilter(OPERATOR.OR, operands))
            }
            case OPERATOR.IN: {
                const v = Array.isArray(value) ? value : String(value as string).split(',')
                const operands = TableQueryFilter.convertOperand(type, v, columnName)
                if (v.length === 1) {
                    return new TableQueryFilter(OPERATOR.EQUAL, operands)
                }
                return new TableQueryFilter(OPERATOR.OR, operands)
            }
            default: {
                break
            }
        }

        const operands = TableQueryFilter.convertOperand(type, value, columnName)
        return new TableQueryFilter(operator, operands)
    }

    static AND(operands) {
        if (!operands) return undefined
        if (operands.length < 2) return operands[0]?.filter
        return new TableQueryFilter(OPERATOR.AND, operands)
    }

    size() {
        return this.operands.length
    }

    toJSON(): { filter: TableQueryFilterDesc } {
        return {
            filter: {
                operator: this.operator,
                operands: this.operands.map((and) => {
                    if (and instanceof TableQueryFilter) return and.toJSON()
                    return and
                }),
            },
        }
    }
}

export { TableQueryFilter }
export default TableQueryFilter
