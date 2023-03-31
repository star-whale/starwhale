import React from 'react'
import { TreeNodeDataT } from '../types'

export function useTreeDataSelection({
    selectedIds: prevSelectedIds,
    onSelectedIdsChange,
    multiple = true,
}: {
    selectedIds: any[]
    onSelectedIdsChange: (ids: any[]) => any[]
    multiple?: boolean
}) {
    return {
        onToggle: React.useCallback(
            (toggleNode: TreeNodeDataT) => {
                if (!toggleNode) return prevSelectedIds

                const $onToggle = (node: TreeNodeDataT) => {
                    let ids = [node.id]

                    if (!multiple) {
                        const index = prevSelectedIds.indexOf(node.id)
                        if (index > -1) {
                            return []
                        }
                        return ids
                    }

                    if (node.isLeafNode) {
                        const childrenCount = node?.children?.length || 0
                        const childrenSelectedCount =
                            node?.children?.filter((child) => prevSelectedIds.includes(child.id)).length || 0
                        const isSelected = childrenSelectedCount > 0 && childrenCount === childrenSelectedCount
                        const isSelectedIndeterminate =
                            childrenSelectedCount > 0 && childrenSelectedCount < childrenCount
                        ids = node.children?.map((child: TreeNodeDataT) => child?.id) ?? []
                        // select all
                        if (isSelectedIndeterminate) {
                            return Array.from(new Set([...prevSelectedIds, ...ids]))
                        }
                        // disselect all
                        if (isSelected) return prevSelectedIds.filter((id: any) => !ids.includes(id))

                        return Array.from(new Set([...prevSelectedIds, ...ids]))
                    }

                    const index = prevSelectedIds.indexOf(node.id)
                    if (index > -1) {
                        prevSelectedIds.splice(index, 1)
                        return [...prevSelectedIds]
                    }

                    return [...prevSelectedIds, node.id]
                }
                const $newIds = $onToggle(toggleNode)
                onSelectedIdsChange?.($newIds)
                return $newIds
            },
            [onSelectedIdsChange, prevSelectedIds, multiple]
        ),
    }
}

export default useTreeDataSelection
