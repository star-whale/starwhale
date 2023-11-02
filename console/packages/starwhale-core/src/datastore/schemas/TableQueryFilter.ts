import { DataTypes, OPERATOR } from '../constants'
import { TableQueryFilterDesc } from './datastore'

type TableQueryOperandT = {
    columnName?: string
    value?: any
    type?: DataTypes
    operator?: OPERATOR
}

export const DATETIME_DELIMITER = '~'
export const INPUT_DELIMITER = ','

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

    static convertOperand(type, value, columnName, operator = OPERATOR.EQUAL) {
        if (value instanceof TableQueryFilter) return value
        if (Array.isArray(value)) {
            if (value.length > 1) {
                return value.map(
                    (v) =>
                        new TableQueryFilter(operator, [{ columnName }, TableQueryFilter.convertOperandType(type, v)])
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
            case OPERATOR.BEFORE: {
                const [before] = (Array.isArray(value) ? value : String(value as string).split(DATETIME_DELIMITER)).map(
                    (v) => (v ? Date.parse(v) : undefined)
                )
                return new TableQueryFilter(OPERATOR.LESS, TableQueryFilter.convertOperand(type, before, columnName))
            }
            case OPERATOR.AFTER: {
                const [before] = (Array.isArray(value) ? value : String(value as string).split(DATETIME_DELIMITER)).map(
                    (v) => (v ? Date.parse(v) : undefined)
                )
                return new TableQueryFilter(OPERATOR.GREATER, TableQueryFilter.convertOperand(type, before, columnName))
            }
            case OPERATOR.BETWEEN: {
                const [before, after] = (
                    Array.isArray(value) ? value : String(value as string).split(DATETIME_DELIMITER)
                ).map((v) => (v ? Date.parse(v) : undefined))
                const beforeFitler = new TableQueryFilter(
                    OPERATOR.GREATER_EQUAL,
                    TableQueryFilter.convertOperand(type, before, columnName)
                )
                const afterFilter = new TableQueryFilter(
                    OPERATOR.LESS_EQUAL,
                    TableQueryFilter.convertOperand(type, after, columnName)
                )
                if (!after) {
                    return beforeFitler
                }

                return new TableQueryFilter(OPERATOR.AND, [beforeFitler, afterFilter])
            }
            case OPERATOR.NOT_BETWEEN: {
                const [before, after] = (
                    Array.isArray(value) ? value : String(value as string).split(DATETIME_DELIMITER)
                ).map((v) => (v ? Date.parse(v) : undefined))
                const beforeFitler = new TableQueryFilter(
                    OPERATOR.LESS_EQUAL,
                    TableQueryFilter.convertOperand(type, before, columnName)
                )
                const afterFilter = new TableQueryFilter(
                    OPERATOR.GREATER_EQUAL,
                    TableQueryFilter.convertOperand(type, after, columnName)
                )
                if (!after) {
                    return beforeFitler
                }

                return new TableQueryFilter(OPERATOR.OR, [beforeFitler, afterFilter])
            }
            case OPERATOR.NOT_IN: {
                const v = Array.isArray(value) ? value : String(value as string).split(INPUT_DELIMITER)
                const operands = TableQueryFilter.convertOperand(type, v, columnName)
                if (v.length === 1) {
                    return new TableQueryFilter(OPERATOR.NOT, new TableQueryFilter(OPERATOR.EQUAL, operands))
                }
                return new TableQueryFilter(OPERATOR.NOT, new TableQueryFilter(OPERATOR.OR, operands))
            }
            case OPERATOR.IN: {
                const v = Array.isArray(value) ? value : String(value as string).split(INPUT_DELIMITER)
                const operands = TableQueryFilter.convertOperand(type, v, columnName)
                if (v.length === 1) {
                    return new TableQueryFilter(OPERATOR.EQUAL, operands)
                }
                return new TableQueryFilter(OPERATOR.OR, operands)
            }
            case OPERATOR.CONTAINS:
            case OPERATOR.NOT_CONTAINS:
                return undefined
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
