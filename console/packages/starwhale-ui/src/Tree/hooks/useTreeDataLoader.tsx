import { TreeNodeData } from '@starwhale/ui/base/tree-view'
import React from 'react'
import { TreeNodeDataT } from '../types'

type TreeDataLoaderT = {
    data: TreeNodeData[]
    search: string
    searchFilter: (search: string, node: TreeNodeData) => boolean
    nodeRender: (node: TreeNodeData) => React.ReactNode
    extraFilters?: ((node: TreeNodeData) => boolean)[]
}

function useTreeDataLoader({ data: $data, search, searchFilter, nodeRender, extraFilters }: TreeDataLoaderT) {
    const walk = (treeNodes: TreeNodeData[], path: any[] = []): TreeNodeDataT[] => {
        return treeNodes
            ?.map((node: TreeNodeData, index: number): TreeNodeDataT => {
                const pathTmp = [...path, index]

                return {
                    id: node.id,
                    label: nodeRender,
                    isExpanded: node.isExpanded,
                    path: pathTmp.join('/'),
                    info: { label: node.label, ...(node.info ?? {}) },
                    isLeafNode: node.children && node.children?.length > 0,
                    children: node.children ? walk(node.children, [...pathTmp, 'children']) : undefined,
                }
            })
            .filter((node: TreeNodeData) => {
                if (node.isLeafNode && node.children?.length !== 0) return true
                if (!search) return true
                return searchFilter(search, node)
            })
            .filter((node: TreeNodeData) => {
                if (node.isLeafNode && node.children?.length !== 0) return true
                if (!extraFilters) return true
                return extraFilters?.every((filter) => filter(node))
            })
            .filter((node: TreeNodeData) => {
                if (node.isLeafNode && node.children?.length === 0) return false
                return true
            })
    }

    // console.log(walk($data))

    return React.useMemo(() => {
        return {
            data: walk($data),
        }
        // eslint-disable-next-line react-hooks/exhaustive-deps
    }, [$data, search, searchFilter, nodeRender])
}

export { useTreeDataLoader }
export default useTreeDataLoader
