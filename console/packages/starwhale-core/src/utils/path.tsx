import { WidgetTreeNode } from '../types'

export function getTreePath(state: any, id: string) {
    let rtn: any = {
        parent: undefined,
        current: undefined,
        children: undefined,
    }

    function walk(nodes: WidgetTreeNode[], paths: any) {
        nodes.forEach((node: WidgetTreeNode, i) => {
            const currPaths: any[] = []
            if (node.id === id) {
                rtn = {
                    parent: ['tree', ...paths],
                    current: ['tree', ...paths, i],
                    children: ['tree', ...paths, i, 'children'],
                }
                return
            }
            currPaths.push(i)
            if (node.children) {
                currPaths.push('children')
                walk(node.children, [...paths, ...currPaths])
            }
        })
    }

    walk(state.tree, [])

    return rtn
}
