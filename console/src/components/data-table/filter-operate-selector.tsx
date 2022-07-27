import { SelectProps } from 'baseui/select'
import React from 'react'
import Input from '../Input'
import Select from '../Select'
// eslint-disable-next-line
import { ColumnT, FilterTypes } from './types'

export interface IFilterOperateSelectorProps {
    columns: ColumnT[]
    value?: FilterOperateSelectorValueT
    onChange?: (newValue: FilterOperateSelectorValueT) => void
    overrides?: SelectProps['overrides']
    disabled?: boolean
}

// is / is not / = / ≠ / in / not in / >= / <= / exists / not exists / contains / not contains
export type FilterOperateSelectorValueT = {
    disable?: boolean
    key?: string
    value?: any
    op?: OperatorT
}

export type OperatorT = {
    label: string
    op: string
    key?: string
    buildFilter?: (args: FilterOperateSelectorValueT) => (data: any, row?: any, column?: any) => any
}

export const Operators: Record<string, OperatorT> = {
    is: {
        key: 'is',
        label: 'is',
        op: '=',
        buildFilter: () => () => true,
    },
    isNot: {
        key: 'isNot',
        label: 'is not',
        op: '!=',
        buildFilter: () => () => true,
    },
    equal: {
        key: 'equal',
        label: '=',
        op: '=',
        buildFilter: ({ value = '' }) => {
            return (data: string) => {
                return value === String(data).trim()
            }
        },
    },
    notEqual: {
        key: 'notEqual',
        label: '≠',
        op: '≠',
        buildFilter: ({ value = '' }) => {
            return (data: string) => {
                return value !== String(data).trim()
            }
        },
    },
    greaterEqual: {
        key: 'greaterEqual',
        label: '>=',
        op: '>=',
        buildFilter: ({ value = '' }) => {
            return (data: number) => {
                return value <= data
            }
        },
    },
    smallerEqual: {
        key: 'smallerEqual',
        label: '<=',
        op: '<=',
        buildFilter: ({ value = '' }) => {
            return (data: number) => {
                return value >= data
            }
        },
    },
    exists: {
        key: 'exists',
        label: 'exists',
        op: 'exists',
        // @ts-ignore
        buildFilter: () => {
            return (data: string, row: any, column: any) => {
                return column.key in row || row?.attributes.map((attr: any) => attr.name).includes(column.key)
            }
        },
    },
    notExists: {
        key: 'notExists',
        label: 'not exists',
        op: 'not exists',
        buildFilter: () => {
            return (data: string, row: any, column: any) => {
                return !(column.key in row || row?.attributes.map((attr: any) => attr.name).includes(column.key))
            }
        },
    },
    contains: {
        key: 'contains',
        label: 'contains',
        op: 'contains',
        buildFilter: ({ value = '' }) => {
            return (data: string) => {
                return data.trim().includes(value)
            }
        },
    },
    notContains: {
        key: 'notContains',
        label: 'not contains',
        op: 'notContains',
        buildFilter: ({ value = '' }) => {
            return (data: string) => {
                return !data.trim().includes(value)
            }
        },
    },
    in: {
        key: 'in',
        label: 'in',
        op: 'in',
        buildFilter: () => () => true,
    },
    notIn: {
        key: 'notIn',
        label: 'not in',
        op: 'not in',
        buildFilter: ({ value = [] }) => {
            return (data) => {
                return !value.has(data)
            }
        },
    },
}

export const FilterTypeOperators = {
    [FilterTypes.sysDefault]: ['equal', 'notEqual', 'contains', 'notContains'],
    [FilterTypes.default]: ['equal', 'notEqual', 'greaterEqual', 'smallerEqual', 'exists', 'notExists'],
    [FilterTypes.enum]: ['equal', 'notEqual'],
    [FilterTypes.string]: ['equal', 'notEqual', 'contains', 'notContains'],
    [FilterTypes.number]: ['equal', 'notEqual', 'greaterEqual', 'smallerEqual', 'exists', 'notExists'],
}

export default function FilterOperateSelector({
    value: raw = {},
    onChange,
    overrides,
    disabled,
    columns,
}: IFilterOperateSelectorProps) {
    // const [key, setKey] = useState<string | undefined>(raw?.key)
    // const [value, setValue] = useState<string | undefined>(raw?.value)
    // const [operator, setOperator] = useState<string | undefined>(raw?.op || '=')

    const { key = '', value = '', op = Operators.equal } = raw

    const $keys = React.useMemo(() => {
        return columns.map((column) => {
            return {
                id: column.key,
                label: column.title,
            }
        })
    }, [columns])

    // eslint-disable-next-line no-param-reassign
    overrides = overrides || {
        Popover: {
            props: {
                overrides: {},
            },
        },
    }

    const $operators = React.useMemo(() => {
        const kind = columns.find((column) => column.key === key)?.filterType
        if (kind && kind in FilterTypeOperators) {
            return FilterTypeOperators?.[kind].map((opV) => Operators[opV])
        }
        return FilterTypeOperators.sysDefault.map((opV) => Operators[opV])
    }, [columns, key])

    return (
        <div style={{ position: 'relative', display: 'grid', gridTemplateColumns: '200px 120px 200px', gap: '8px' }}>
            <Select
                size='compact'
                disabled={disabled}
                overrides={overrides}
                options={$keys}
                placeholder='-'
                clearable={false}
                onChange={(params) => {
                    if (!params.option) {
                        return
                    }
                    onChange?.({ value: '', op, key: params.option?.id as string })
                }}
                value={key ? [{ id: key }] : []}
            />
            <Select
                size='compact'
                disabled={disabled}
                overrides={overrides}
                placeholder='='
                clearable={false}
                options={$operators.map((item) => ({
                    id: item.key,
                    label: item.label,
                }))}
                onChange={(params) => {
                    if (!params.option) {
                        return
                    }
                    onChange?.({ value, key, op: Operators?.[params.option.id as string] })
                }}
                value={op ? [{ id: op.key }] : []}
            />
            {!['exists', 'notExists'].includes(op.key as string) && (
                <Input
                    size='compact'
                    disabled={disabled}
                    overrides={overrides}
                    placeholder=''
                    clearable={false}
                    onChange={(e: any) => {
                        onChange?.({ key, op, value: e.target.value as string })
                    }}
                    value={value}
                />
            )}
            {/* <Select
                size='compact'
                disabled={disabled}
                overrides={overrides}
                options={options}
                placeholder='-'
                clearable={false}
                onChange={({ value }) => {
                    // @ts-ignore
                    setValue(value)
                }}
                mountNode={document.body}
                onInputChange={(e) => {
                    const target = e.target as HTMLInputElement
                }}
                onBlurResetsInput={false}
                value={
                    value
                        ? [
                              {
                                  id: value,
                              },
                          ]
                        : []
                }
            /> */}
        </div>
    )
}
