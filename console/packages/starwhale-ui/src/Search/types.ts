import { DATETIME_DELIMITER } from '@starwhale/core/datastore/schemas/TableQueryFilter'
import moment from 'moment'
import _ from 'lodash'

export enum KIND {
    BOOLEAN = 'BOOLEAN',
    CATEGORICAL = 'CATEGORICAL',
    CUSTOM = 'CUSTOM',
    DATETIME = 'DATETIME',
    NUMERICAL = 'NUMERICAL',
    STRING = 'STRING',
    DATATIME = 'DATATIME',
    // only for local search
    STRING_WITH_CONTAINS = 'STRING_WITH_CONTAINS',
}

export enum OPERATOR {
    EQUAL = 'EQUAL',
    GREATER = 'GREATER',
    GREATER_EQUAL = 'GREATER_EQUAL',
    LESS = 'LESS',
    LESS_EQUAL = 'LESS_EQUAL',
    NOT = 'NOT',
    IN = 'IN',
    NOT_IN = 'NOT_IN',
    OR = 'OR',
    AND = 'AND',
    CONTAINS = 'CONTAINS',
    NOT_CONTAINS = 'NOT_CONTAINS',
    // IS = 'IS',
    // IS_NOT = 'IS_NOT',
    EXISTS = 'EXISTS',
    NOT_EXISTS = 'NOT_EXISTS',
    // datatime
    BEFORE = 'BEFORE',
    AFTER = 'AFTER',
    BETWEEN = 'BETWEEN',
    NOT_BETWEEN = 'NOT_BETWEEN',
}

export const FilterTypeOperators: Record<Partial<KIND>, OPERATOR[]> = {
    [KIND.CATEGORICAL]: [],
    [KIND.STRING]: [OPERATOR.EQUAL, OPERATOR.IN, OPERATOR.NOT_IN],
    [KIND.NUMERICAL]: [
        OPERATOR.EQUAL,
        OPERATOR.GREATER,
        OPERATOR.GREATER_EQUAL,
        OPERATOR.LESS,
        OPERATOR.LESS_EQUAL,
        OPERATOR.IN,
        OPERATOR.NOT_IN,
    ],
    [KIND.DATATIME]: [OPERATOR.EQUAL, OPERATOR.BEFORE, OPERATOR.AFTER, OPERATOR.BETWEEN, OPERATOR.NOT_BETWEEN],
    BOOLEAN: [OPERATOR.EQUAL],
    CUSTOM: [],
    DATETIME: [],
    STRING_WITH_CONTAINS: [OPERATOR.EQUAL, OPERATOR.CONTAINS, OPERATOR.NOT_CONTAINS, OPERATOR.IN, OPERATOR.NOT_IN],
}

export const Operators: Record<string, OperatorT> = {
    [OPERATOR.EQUAL]: {
        key: OPERATOR.EQUAL,
        label: '=',
        value: '=',
        // @ts-ignore
        buildFilter: ({ value = '' }) => {
            return (data: string) => {
                return value === String(data).trim()
            }
        },
    },
    [OPERATOR.NOT]: {
        key: OPERATOR.NOT,
        label: '≠',
        value: '≠',
        // @ts-ignore
        buildFilter: ({ value = '' }) => {
            return (data: string) => {
                return value !== String(data).trim()
            }
        },
    },
    [OPERATOR.GREATER]: {
        key: OPERATOR.GREATER,
        label: '>',
        value: '>',
        // @ts-ignore
        buildFilter: ({ value = 0 }) => {
            return (data: number) => {
                return value < data
            }
        },
    },
    [OPERATOR.GREATER_EQUAL]: {
        key: OPERATOR.GREATER_EQUAL,
        label: '>=',
        value: '>=',
        // @ts-ignore
        buildFilter: ({ value = 0 }) => {
            return (data: number) => {
                return value <= data
            }
        },
    },
    [OPERATOR.LESS]: {
        key: OPERATOR.LESS,
        label: '<',
        value: '<',
        // @ts-ignore
        buildFilter: ({ value = 0 }) => {
            return (data: number) => {
                return value > data
            }
        },
    },
    [OPERATOR.LESS_EQUAL]: {
        key: OPERATOR.LESS_EQUAL,
        label: '<=',
        value: '<=',
        // @ts-ignore
        buildFilter: ({ value = 0 }) => {
            return (data: number) => {
                return value >= data
            }
        },
    },
    [OPERATOR.EXISTS]: {
        key: OPERATOR.EXISTS,
        label: 'exists',
        value: 'exists',
        // @ts-ignore
        buildFilter: () => {
            return (data: string, row: any, column: any) => {
                return column.key in row || column.key in row?.attributes
            }
        },
    },
    [OPERATOR.NOT_EXISTS]: {
        key: OPERATOR.NOT_EXISTS,
        label: 'not exists',
        value: 'not exists',
        // @ts-ignore
        buildFilter: () => {
            return (data: string, row: any, column: any) => {
                return !(column.key in row) && !(column.key in row?.attributes)
            }
        },
    },
    [OPERATOR.CONTAINS]: {
        key: OPERATOR.CONTAINS,
        label: 'contains',
        value: 'contains',
        // @ts-ignore
        buildFilter: ({ value = '' }) => {
            return (data: string) => {
                return String(data ?? '')
                    .trim()
                    .includes(value)
            }
        },
    },
    [OPERATOR.NOT_CONTAINS]: {
        key: OPERATOR.NOT_CONTAINS,
        label: 'not contains',
        value: 'notContains',
        // @ts-ignore
        buildFilter: ({ value = '' }) => {
            return (data: string) => {
                return !data.trim().includes(value)
            }
        },
    },
    [OPERATOR.IN]: {
        key: OPERATOR.IN,
        label: 'in',
        value: 'in',
        // @ts-ignore
        buildFilter: ({ value = [] }) => {
            return (data) => {
                return value.includes(data)
            }
        },
    },
    [OPERATOR.NOT_IN]: {
        key: OPERATOR.NOT_IN,
        label: 'not in',
        value: 'not in',
        // @ts-ignore
        buildFilter: ({ value = [] }) => {
            return (data) => {
                return !value.includes(data)
            }
        },
    },
    [OPERATOR.BEFORE]: {
        key: OPERATOR.BEFORE,
        label: 'before',
        value: 'before',
        // @ts-ignore
        buildFilter: ({ value }) => {
            return (data: number) => {
                return data < moment(value).valueOf()
            }
        },
    },
    [OPERATOR.AFTER]: {
        key: OPERATOR.AFTER,
        label: 'after',
        value: 'after',
        // @ts-ignore
        buildFilter: ({ value }) => {
            return (data: number) => {
                return data > moment(value).valueOf()
            }
        },
    },
    [OPERATOR.BETWEEN]: {
        key: OPERATOR.BETWEEN,
        label: 'between',
        value: 'between',
        // @ts-ignore
        buildFilter: ({ value: _v }) => {
            const [before, after] = _.isString(_v) ? _v.split(DATETIME_DELIMITER) : _v
            return (data: number) => {
                return data > moment(before).valueOf() && data < moment(after).valueOf()
            }
        },
    },
    [OPERATOR.NOT_BETWEEN]: {
        key: OPERATOR.NOT_BETWEEN,
        label: 'not between',
        value: 'not between',
        // @ts-ignore
        buildFilter: ({ value: _v }) => {
            const [before, after] = _.isString(_v) ? _v.split(DATETIME_DELIMITER) : _v
            return (data: number) => {
                return data < moment(before).valueOf() || data > moment(after).valueOf()
            }
        },
    },
}

export type SearchFieldSchemaT = {
    id: string
    type: string
    label: string
    name: string
}

export type FilterSharedPropsT = {
    isDisabled?: boolean
    isEditing?: boolean
    isFocus?: boolean
}

export interface FilterPropsT extends FilterSharedPropsT {
    value?: ValueT
    onChange?: (newValue?: ValueT) => void
    options?: any[]
    onItemEditing?: (args: boolean) => void
}

export type ValueT = {
    property?: string
    op?: string
    value?: any
}

export type FilterValueT = {
    data: ValueT
} & FilterSharedPropsT

export interface FilterRenderPropsT extends FilterSharedPropsT {
    value?: string
    onChange?: (newValue?: string) => void
    options?: any[]
    mountNode?: HTMLElement
    inputRef?: React.RefObject<any>
    renderInput?: () => React.ReactNode
    renderAfter?: () => React.ReactNode
    optionFilter?: (tmp: any) => boolean
}

export type FilterT = {
    key?: string
    label?: string
    kind?: KIND
    operatorOptions?: any[]
    fieldOptions?: any[]
    valueOptions?: any[]

    renderField?: React.FC<FilterRenderPropsT>
    renderFieldLabel?: React.FC<FilterRenderPropsT>
    renderFieldValue?: React.FC<FilterRenderPropsT>
    renderOperator?: React.FC<FilterRenderPropsT>
    renderValue?: React.ForwardedRef<FilterRenderPropsT>
}

export type OperatorT = {
    label: string
    value: string
    key?: string
    // @ts-ignore
    buildFilter?: (args: FilterValueT) => (data: any) => any
}
