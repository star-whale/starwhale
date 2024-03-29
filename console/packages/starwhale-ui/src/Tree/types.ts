import { TreeNodeData } from '../base/tree-view'

export type TreeNodeDataT = TreeNodeData<TreeNodeInfoT> & TreeNodeDataExtendT

export type TreeNodeInfoT = {
    label?: React.ReactNode
    labelView?: React.ReactNode
    labelTitle?: string
}

export type TreeNodeDataExtendT = {
    path: string
    isSelected?: boolean
    isLeafNode?: boolean
    isShow?: boolean
}

export type TreePropsT = {
    data: TreeNodeData[]
    searchable?: boolean
    search?: string

    selectable?: boolean
    selectedIds?: any[]
    onSelectedIdsChange?: (selectedIds: any[]) => any

    multiple?: boolean
    keyboardControlNode?: { current: null }

    renderLabel?: (node: TreeNodeData) => React.ReactNode
    renderActions?: (node: TreeNodeData) => React.ReactNode
}

export type TreeNodePropsT = {
    node: TreeNodeData
    multiple?: boolean
    selectable?: boolean
    isSelected?: boolean
    isSelectedIndeterminate?: boolean
    onChange?: (bool: boolean) => void
    actions?: React.ReactNode
    label: React.ReactNode
}
