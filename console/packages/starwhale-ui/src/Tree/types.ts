import { TreeNodeData } from '../base/tree-view'

export type TreeNodeDataT = TreeNodeData<TreeNodeInfoT> & TreeNodeDataExtendT

export type TreeNodeInfoT = {
    label: string
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

    selectable?: boolean
    selectedIds?: any[]
    onSelectedIdsChange?: (selectedIds: any[]) => any

    multiple?: boolean
}

export type TreeNodePropsT = {
    node: TreeNodeData
    label: TreeNodeData
    multiple?: boolean
    selectable?: boolean
    isSelected?: boolean
    isSelectedIndeterminate?: boolean
    onChange?: (bool: boolean) => void
    actions?: React.ReactNode
}
