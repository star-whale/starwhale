import { TreeNodeData } from '../base/tree-view'

export type TreeNodeDataT = TreeNodeData<TreeNodeInfoT> & TreeNodeDataExtendT

export type TreeNodeInfoT = {
    label: string
}

export type TreeNodeDataExtendT = {
    path: string
    isSelected?: boolean
    isLeafNode?: boolean
}

export type TreePropsT = {
    data: TreeNodeData[]
    searchable?: boolean

    selectable?: boolean
    selectedIds?: any[]
    onSelectedIdsChange?: (selectedIds: any[]) => void

    muliple?: boolean
}
