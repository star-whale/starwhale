import React, { useEffect } from 'react'
import { toggleIsExpanded, TreeNodeData, TreeView } from '../base/tree-view'
import { TreeContainer, TreeSearch } from './StyledComponent'
import { TreeNodeDataT, TreePropsT } from './types'
import TreeNode from './TreeNode'

type TreeDataLoaderT = {
    search: string
    searchFilter: (search: string, node: TreeNodeData) => boolean
    nodeRender: (node: TreeNodeData) => React.ReactNode
}

function useTreeDataLoader({ data: $data, search, searchFilter, nodeRender }: TreeDataLoaderT) {
    const walk = (treeNodes: TreeNodeData[], path: any[] = []): TreeNodeDataT[] => {
        return treeNodes
            ?.map((node: TreeNodeData, index: number): TreeNodeDataT => {
                const pathTmp = [...path, index]

                return {
                    id: node.id,
                    label: nodeRender,
                    isExpanded: node.isExpanded,
                    path: pathTmp.join('/'),
                    info: node.info ?? { label: node.label },
                    isLeafNode: node.children && node.children?.length > 0,
                    children: node.children ? walk(node.children, [...pathTmp, 'children']) : undefined,
                }
            })
            .filter((node: TreeNodeData) => {
                if (node.isLeafNode) return true
                if (!search) return true

                return searchFilter(search, node)
            })
    }

    return React.useMemo(() => {
        return {
            data: walk($data),
        }
        // eslint-disable-next-line react-hooks/exhaustive-deps
    }, [$data, search, searchFilter, nodeRender])
}

function useTreeDataSelection({
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

                    if (!multiple) {
                        return prevSelectedIds[0]
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

const searchFilter = (searchTmp: string, node: TreeNodeDataT) =>
    node.info?.label?.toLocaleLowerCase().includes(searchTmp.toLowerCase())

export function Tree({
    data: rawData,
    searchable = true,
    selectable = true,
    selectedIds = [],
    onSelectedIdsChange = () => {},
    renderLabel = (node: TreeNodeDataT) => node?.info.label,
    renderActions = (node: TreeNodeDataT) => null,
    multiple = true,
    search: rawSearch,
    keyboardControlNode,
}: TreePropsT) {
    const [data, setData] = React.useState(rawData)
    const [search, setSearch] = React.useState('')
    const { onToggle: $onSelectToggle } = useTreeDataSelection({ selectedIds, onSelectedIdsChange, multiple })

    useEffect(() => {
        setSearch(rawSearch ?? '')
    }, [rawSearch])

    const nodeRender = React.useCallback(
        (node: TreeNodeDataT) => {
            let isSelected = selectedIds.includes(node?.id)
            let isSelectedIndeterminate = false
            if (node?.isLeafNode) {
                const childrenCount = node?.children?.length || 0
                const childrenSelectedCount =
                    node?.children?.filter((child) => selectedIds.includes(child.id)).length || 0

                isSelected = childrenSelectedCount > 0 && childrenCount === childrenSelectedCount
                isSelectedIndeterminate = childrenSelectedCount > 0 && childrenSelectedCount < childrenCount
            }

            return (
                <TreeNode
                    node={node}
                    selectable={selectable}
                    isSelected={isSelected}
                    isSelectedIndeterminate={isSelectedIndeterminate}
                    multiple={multiple}
                    label={renderLabel(node)}
                    actions={renderActions(node)}
                    onChange={() => $onSelectToggle(node)}
                />
            )
        },
        [selectable, selectedIds, multiple, renderLabel, renderActions, $onSelectToggle]
    )

    const { data: $data } = useTreeDataLoader({
        data,
        search,
        searchFilter,
        selectedIds,
        nodeRender,
    })

    useEffect(() => {
        setData(rawData)
    }, [rawData])

    const onChange = (e: React.ChangeEvent<HTMLInputElement>) => setSearch?.(e.target.value)
    const onToggle = (node: any) => setData((prevData) => toggleIsExpanded(prevData, node) as any)

    return (
        <TreeContainer>
            {searchable && <TreeSearch value={search} onChange={onChange} />}
            <TreeView
                data={$data}
                onToggle={onToggle}
                keyboardControlNode={keyboardControlNode}
                onSelect={$onSelectToggle}
            />
        </TreeContainer>
    )
}

export default Tree
