import { OperatorT } from './types'

export enum KIND {
    BOOLEAN = 'BOOLEAN',
    CATEGORICAL = 'CATEGORICAL',
    CUSTOM = 'CUSTOM',
    DATETIME = 'DATETIME',
    NUMERICAL = 'NUMERICAL',
    STRING = 'STRING',
}

export enum OPERATOR {
    EQUAL = 'EQUAL',
    GREATER_THAN = 'GREATER_THAN',
    GREATER_THAN_OR_EQUAL = 'GREATER_THAN_OR_EQUAL',
    LESS_THAN = 'LESS_THAN',
    LESS_THAN_OR_EQUAL = 'LESS_THAN_OR_EQUAL',
    NOT_EQUAL = 'NOT_EQUAL',
    IN = 'IN',
    NOT_IN = 'NOT_IN',
    CONTAINS = 'CONTAINS',
    NOT_CONTAINS = 'NOT_CONTAINS',
    IS = 'IS',
    IS_NOT = 'IS_NOT',
    EXISTS = 'EXISTS',
    NOT_EXISTS = 'NOT_EXISTS',
}

export const FilterTypeOperators: Record<Partial<KIND>, OPERATOR[]> = {
    [KIND.CATEGORICAL]: [OPERATOR.EQUAL, OPERATOR.NOT_EQUAL, OPERATOR.IN, OPERATOR.NOT_IN],
    [KIND.STRING]: [OPERATOR.EQUAL, OPERATOR.NOT_EQUAL, OPERATOR.CONTAINS, OPERATOR.NOT_CONTAINS],
    [KIND.NUMERICAL]: [
        OPERATOR.EQUAL,
        OPERATOR.NOT_EQUAL,
        OPERATOR.GREATER_THAN,
        OPERATOR.GREATER_THAN_OR_EQUAL,
        OPERATOR.LESS_THAN,
        OPERATOR.LESS_THAN_OR_EQUAL,
    ],
    BOOLEAN: [OPERATOR.EQUAL, OPERATOR.NOT_EQUAL],
    CUSTOM: [],
    DATETIME: [],
}

export const Operators: Record<string, OperatorT> = {
    [OPERATOR.IS]: {
        key: OPERATOR.IS,
        label: 'is',
        value: '=',
        buildFilter: () => () => true,
    },
    [OPERATOR.IS_NOT]: {
        key: OPERATOR.IS_NOT,
        label: 'is not',
        value: '!=',
        buildFilter: () => () => true,
    },
    [OPERATOR.EQUAL]: {
        key: OPERATOR.EQUAL,
        label: '=',
        value: '=',
        buildFilter: ({ value = '' }) => {
            return (data: string) => {
                return value === String(data).trim()
            }
        },
    },
    [OPERATOR.NOT_EQUAL]: {
        key: OPERATOR.NOT_EQUAL,
        label: '≠',
        value: '≠',
        buildFilter: ({ value = '' }) => {
            return (data: string) => {
                return value !== String(data).trim()
            }
        },
    },
    [OPERATOR.GREATER_THAN_OR_EQUAL]: {
        key: OPERATOR.GREATER_THAN_OR_EQUAL,
        label: '>=',
        value: '>=',
        buildFilter: ({ value = 0 }) => {
            return (data: number) => {
                return value <= data
            }
        },
    },
    [OPERATOR.LESS_THAN_OR_EQUAL]: {
        key: OPERATOR.LESS_THAN_OR_EQUAL,
        label: '<=',
        value: '<=',
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
        buildFilter: () => () => true,
    },
    [OPERATOR.NOT_IN]: {
        key: OPERATOR.NOT_IN,
        label: 'not in',
        value: 'not in',
        buildFilter: ({ value = [] }) => {
            return (data) => {
                return !value.has(data)
            }
        },
    },
}
