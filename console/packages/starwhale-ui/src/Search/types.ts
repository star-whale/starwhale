import { OPERATOR } from '@starwhale/core/datastore'
import { KIND } from './constants'

export type KindT = keyof typeof KIND

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

export type OperatorT = {
    label: string
    value: string
    key?: string
    buildFilter?: (args: FilterValueT) => (data: any) => any
}

export interface FilterRenderPropsT extends FilterSharedPropsT {
    value?: ValueT
    onChange?: (newValue?: string) => void
    options?: any[]
}

export type FilterT = {
    key?: string
    label?: string
    kind: KindT
    operators: OPERATOR[]

    // buildFilter?: (args: FilterValueT<ValueT>) => (data: any) => any
    renderField?: (args: FilterRenderPropsT) => React.ReactElement
    renderFieldValue?: (args: FilterRenderPropsT) => React.ReactElement
    renderOperator?: (args: FilterRenderPropsT) => React.ReactElement
}
