export interface DynamicSelectorPropsT<T> {
    value?: SelectorItemValueT[]
    onChange?: (args: SelectorItemValueT[]) => void
    startEnhancer?: () => React.ReactNode
    placeholder?: React.ReactNode
    options?: SelectorItemOptionT<T>[]
}

export type SelectorItemValueT = {
    id?: string | number
    value?: any
}

export type SelectorItemOptionT<T = any> = {
    id?: string | number
    label: ((item: SelectorItemOptionT) => React.ReactNode) | string
    component: (item: SelectorItemOptionT) => React.FC<any>
    info?: T
    [key: string]: any
}

export type SelectorSharedPropsT = {
    $isEditing?: boolean
}

export type SelectorItemRenderPropsT = {
    value?: SelectorItemValueT
    onChange?: (args: SelectorItemValueT) => void
    options?: SelectorItemOptionT[]
}

export type SelectorItemPropsT = {
    value?: SelectorItemValueT
    onChange?: (args: SelectorItemValueT) => void
    search?: string
    inputRef?: React.RefObject<HTMLInputElement>
} & SelectorSharedPropsT
