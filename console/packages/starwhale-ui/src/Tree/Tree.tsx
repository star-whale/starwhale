import React, { useEffect } from 'react'
import { toggleIsExpanded, TreeNodeData, TreeView } from '../base/tree-view'
import { TreeContainer, TreeSearch } from './StyledComponent'
import { TreeNodeDataT, TreeNodeT, TreePropsT } from './types'
import TreeNodeCheckbox from './TreeNodeCheckbox'

const treeData = [
    {
        id: '1',
        label: 'Fruit',
        isExpanded: true,
        info: { label: 'Fruit' },
        children: [
            {
                id: '2',
                label: 'Apple',
                isExpanded: true,
                children: [],
            },

            {
                id: '3',
                label: 'Apple',
                isExpanded: true,
                children: [],
            },
        ],
    },
]

type TreeDataLoaderT = {
    search: string
    searchFilter: (search: string, node: TreeNodeData) => boolean
    nodeRender: (node: TreeNodeData) => React.ReactNode
}

function useTreeDataLoader({ search, searchFilter, nodeRender }: TreeDataLoaderT) {
    const walk = (treeNodes: TreeNodeData[], path: any[] = []): TreeNodeDataT[] => {
        return treeNodes
            .map((node: TreeNodeData, index: number): TreeNodeDataT => {
                const pathTmp = [...path, index]

                return {
                    id: node.id,
                    label: nodeRender,
                    isExpanded: true,
                    path: pathTmp.join('/'),
                    info: node.info ?? { label: node.label },
                    children: node.children ? walk(node.children, [...pathTmp, 'children']) : undefined,
                }
            })
            .filter((node: TreeNodeData) => searchFilter(search, node))
    }

    return React.useMemo(() => {
        return {
            data: walk(treeData),
        }
        // eslint-disable-next-line react-hooks/exhaustive-deps
    }, [search, searchFilter, nodeRender])
}

function useTreeDataSelection({ selectids, onSelectedIdsChange, muliple = true }: { muliple?: boolean }) {
    const onToggle = (node: TreeNodeData) => {
        if (!muliple && node.children) {
            onSelectedIdsChange([node.id])
            return
        }
        console.log(node.id, 'on')

        if (node.children) {
            onSelectedIdsChange((prevSelectedIds) => {
                const index = prevSelectedIds.indexOf(node.id)
                if (index > -1) {
                    prevSelectedIds.splice(index, 1)
                    return [...prevSelectedIds]
                }

                return [...prevSelectedIds, node.id]
            })
        } else {
            onSelectedIdsChange([node.id])
        }
    }

    return { onToggle }
}

function Tree({
    data: rawData = [],
    searchable = true,
    selectable = true,
    selectedIds = ['2'],
    onSelectedIdsChange = () => {},
    labelRender = (node) => node?.info.label,
    muliple = true,
}: TreePropsT) {
    const [data, setData] = React.useState(treeData)
    const [search, setSearch] = React.useState('')
    const { onToggle: $onSelectToggle } = useTreeDataSelection({
        selectedIds,
        onSelectedIdsChange,
        muliple,
    })

    console.log(selectedIds)

    const { data: $data } = useTreeDataLoader({
        search,
        searchFilter: (searchTmp: string, node: TreeNodeDataT) => node.info?.label?.includes(searchTmp),
        selectedIds,
        nodeRender: (node: TreeNodeData) => {
            if (!selectable) return node?.info.label
            if (muliple)
                return (
                    <TreeNodeCheckbox
                        value={selectedIds.includes(node?.id)}
                        label={labelRender(node)}
                        onChange={() => $onSelectToggle(node)}
                    />
                )
        },
    })

    // useEffect(() => {
    //     setData(rawData)
    // }, [rawData])

    const onChange = (e: React.ChangeEvent<HTMLInputElement>) => setSearch?.(e.target.value)
    const onToggle = (node: any) => setData((prevData) => toggleIsExpanded(prevData, node) as any)

    console.log(data)

    return (
        <TreeContainer>
            {searchable && <TreeSearch value={search} onChange={onChange} />}
            <TreeView data={$data} onToggle={onToggle} />
        </TreeContainer>
    )
}

export default Tree
