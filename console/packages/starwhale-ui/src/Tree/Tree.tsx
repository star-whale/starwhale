import React, { useEffect } from 'react'
import { toggleIsExpanded, TreeView } from '../base/tree-view'
import { TreeContainer, TreeSearch } from './StyledComponent'
import { TreeNodeDataT, TreePropsT } from './types'
import TreeNode from './TreeNode'
import useTreeDataSelection from './hooks/useTreeDataSelection'
import useTreeDataLoader from './hooks/useTreeDataLoader'
import { expandMargin } from '../utils/index'
import { useMount } from 'react-use'

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
    SearchSlot,
}: TreePropsT & {
    SearchSlot?: React.FC<any>
}) {
    const [data, setData] = React.useState(rawData)
    const [search, setSearch] = React.useState('')
    const { onToggle: $onSelectToggle } = useTreeDataSelection({ selectedIds, onSelectedIdsChange, multiple })

    useEffect(() => {
        setSearch(rawSearch ?? '')
    }, [rawSearch])

    useEffect(() => {
        setData(rawData)
    }, [rawData])

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
    const [extraFilters, setExtraFilters] = React.useState<any[]>([])
    const { data: $data } = useTreeDataLoader({
        data,
        search,
        searchFilter,
        extraFilters,
        nodeRender,
    })

    const onChange = (e: React.ChangeEvent<HTMLInputElement>) => setSearch?.(e.target.value)
    const onToggle = (node: any) => setData((prevData) => toggleIsExpanded(prevData, node) as any)
    const inputRef = React.useRef<HTMLInputElement>(null)
    const [extraValue, setExtraValue] = React.useState<any>()
    const onExtraFiltersChange = React.useCallback((key, filters) => {
        setExtraValue(key)
        setExtraFilters(filters)
    }, [])

    useMount(() => {
        if (inputRef.current) inputRef.current.focus()
    })

    return (
        <TreeContainer>
            {SearchSlot && <SearchSlot value={extraValue} onChange={onExtraFiltersChange} />}
            {searchable && <TreeSearch value={search} onChange={onChange} inputRef={inputRef} />}
            <div className='flex flex-column overflow-auto flex-1 scroller-sm'>
                <TreeView
                    data={$data}
                    onToggle={onToggle}
                    // @ts-ignore
                    keyboardControlNode={keyboardControlNode?.current ? keyboardControlNode : inputRef}
                    onSelect={$onSelectToggle}
                    overrides={{
                        Root: {
                            style: {
                                ...expandMargin('0', 'auto', 'auto', '0'),
                            },
                        },
                        LeafIconContainer: () => (multiple ? <p className='w-12px' /> : <p className='ml-0px' />),
                    }}
                />
            </div>
        </TreeContainer>
    )
}

export default Tree
