import { OPERATOR } from '@starwhale/core/datastore'

export enum KIND {
    BOOLEAN = 'BOOLEAN',
    CATEGORICAL = 'CATEGORICAL',
    CUSTOM = 'CUSTOM',
    DATETIME = 'DATETIME',
    NUMERICAL = 'NUMERICAL',
    STRING = 'STRING',
}

export type KindT = keyof typeof KIND

export const FilterTypeOperators: Record<Partial<KIND>, OPERATOR[]> = {
    [KIND.CATEGORICAL]: [],
    [KIND.STRING]: [OPERATOR.EQUAL],
    [KIND.NUMERICAL]: [OPERATOR.EQUAL, OPERATOR.GREATER, OPERATOR.GREATER_EQUAL, OPERATOR.LESS, OPERATOR.LESS_EQUAL],
    BOOLEAN: [OPERATOR.EQUAL],
    CUSTOM: [],
    DATETIME: [],
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

export type SearchFieldSchemaT = {
    name: string
    type: string
    path: string
    label: string
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
}

export type FilterT = {
    key?: string
    label?: string
    kind: KindT
    operators: OPERATOR[]

    renderField?: React.FC<FilterRenderPropsT>
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
