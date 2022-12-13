import { FilterT, OperatorT } from './types'
import FilterString from './filterString'
import FilterBoolean from './filterBoolean'
import FilterNumberical from './filterNumberical'
import { DataTypeT } from '../../../starwhale-core/src/datastore/types'
import { OPERATOR } from '@starwhale/core/datastore'

export enum KIND {
    BOOLEAN = 'BOOLEAN',
    CATEGORICAL = 'CATEGORICAL',
    CUSTOM = 'CUSTOM',
    DATETIME = 'DATETIME',
    NUMERICAL = 'NUMERICAL',
    STRING = 'STRING',
}

export const FilterTypeOperators: Record<Partial<KIND>, OPERATOR[]> = {
    [KIND.CATEGORICAL]: [OPERATOR.EQUAL, OPERATOR.NOT, OPERATOR.IN, OPERATOR.NOT_IN],
    [KIND.STRING]: [OPERATOR.EQUAL, OPERATOR.NOT, OPERATOR.EXISTS, OPERATOR.NOT_EXISTS],
    [KIND.NUMERICAL]: [
        OPERATOR.EQUAL,
        OPERATOR.NOT,
        OPERATOR.GREATER,
        OPERATOR.GREATER_EQUAL,
        OPERATOR.LESS,
        OPERATOR.LESS_EQUAL,
        OPERATOR.EXISTS,
        OPERATOR.NOT_EXISTS,
    ],
    BOOLEAN: [OPERATOR.EQUAL, OPERATOR.NOT],
    CUSTOM: [],
    DATETIME: [],
}

export const dataStoreToFilter = (dataStoreKind?: DataTypeT): (() => FilterT) => {
    switch (dataStoreKind) {
        default:
        case 'STRING':
            return FilterString
        case 'BOOL':
            return FilterBoolean
        case 'INT8':
        case 'INT16':
        case 'INT32':
        case 'INT64':
        case 'FLOAT16':
        case 'FLOAT32':
        case 'FLOAT64':
            return FilterNumberical
    }
}

export const dataStoreToFilterKind = (dataStoreKind: DataTypeT): KIND => {
    switch (dataStoreKind) {
        case 'BOOL':
            return KIND.BOOLEAN
        case 'INT8':
        case 'INT16':
        case 'INT32':
        case 'INT64':
        case 'FLOAT16':
        case 'FLOAT32':
        case 'FLOAT64':
            return KIND.NUMERICAL
        case 'STRING':
            return KIND.STRING
        default:
            return KIND.STRING
    }
}

export const Operators: Record<string, OperatorT> = {
    // [OPERATOR.IS]: {
    //     key: OPERATOR.IS,
    //     label: 'is',
    //     value: '=',
    //     buildFilter: () => () => true,
    // },
    // [OPERATOR.IS_NOT]: {
    //     key: OPERATOR.IS_NOT,
    //     label: 'is not',
    //     value: '!=',
    //     buildFilter: () => () => true,
    // },
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
    [OPERATOR.NOT]: {
        key: OPERATOR.NOT,
        label: '≠',
        value: '≠',
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
    // [OPERATOR.CONTAINS]: {
    //     key: OPERATOR.CONTAINS,
    //     label: 'contains',
    //     value: 'contains',
    //     buildFilter: ({ value = '' }) => {
    //         return (data: string) => {
    //             return String(data ?? '')
    //                 .trim()
    //                 .includes(value)
    //         }
    //     },
    // },
    // [OPERATOR.NOT_CONTAINS]: {
    //     key: OPERATOR.NOT_CONTAINS,
    //     label: 'not contains',
    //     value: 'notContains',
    //     buildFilter: ({ value = '' }) => {
    //         return (data: string) => {
    //             return !data.trim().includes(value)
    //         }
    //     },
    // },
    // [OPERATOR.IN]: {
    //     key: OPERATOR.IN,
    //     label: 'in',
    //     value: 'in',
    //     buildFilter: () => () => true,
    // },
    // [OPERATOR.NOT_IN]: {
    //     key: OPERATOR.NOT_IN,
    //     label: 'not in',
    //     value: 'not in',
    //     buildFilter: ({ value = [] }) => {
    //         return (data) => {
    //             return !value.has(data)
    //         }
    //     },
    // },
}
