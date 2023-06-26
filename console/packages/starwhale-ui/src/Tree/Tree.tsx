import React, { useEffect } from 'react'
import { toggleIsExpanded, TreeView } from '../base/tree-view'
import { TreeContainer, TreeSearch } from './StyledComponent'
import { TreeNodeDataT, TreePropsT } from './types'
import TreeNode from './TreeNode'
import useTreeDataSelection from './hooks/useTreeDataSelection'
import useTreeDataLoader from './hooks/useTreeDataLoader'

const searchFilter = (searchTmp: string, node: TreeNodeDataT) => {
    if (typeof node.info?.labelTitle !== 'string') return false
    const searchTerm = searchTmp?.toLowerCase() ?? ''
    const label = node.info?.labelTitle?.toLowerCase() ?? ''
    return label.includes(searchTerm)
}

export function Tree({
    data: rawData,
    searchable = true,
    selectable = true,
    selectedIds = [],
    onSelectedIdsChange = () => {},
    renderLabel = (node: TreeNodeDataT) => node.info?.label,
    renderActions = () => null,
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
