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
    // @ts-ignore
    buildFilter?: (args: FilterValueT) => (data: any) => any
}

export interface FilterRenderPropsT extends FilterSharedPropsT {
    value?: string
    onChange?: (newValue?: string) => void
    options?: any[]
    mountNode?: HTMLElement
    innerRef?: React.RefObject<any>
}

export type FilterT = {
    key?: string
    label?: string
    kind: KindT
    operators: OPERATOR[]

    renderField?: React.FC<FilterRenderPropsT>
    renderFieldValue?: React.FC<FilterRenderPropsT>
    renderOperator?: React.FC<FilterRenderPropsT>
}
