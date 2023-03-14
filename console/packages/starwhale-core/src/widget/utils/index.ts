import { WidgetTreeNode } from '../../types'
import WidgetFactory from '../WidgetFactory'

export const tranformState = (state: {
    key: string
    tree: WidgetTreeNode[]
    defaults: Record<string, any>
    widgets: Record<string, any>
}) => {
    const defaults = {} as any
    const widgets = {} as any

    function walk(nodes: WidgetTreeNode[]) {
        return nodes.map((node: WidgetTreeNode) => {
            const { type, children, ...rest } = node
            // @ts-ignore
            // eslint-disable-next-line no-param-reassign
            if (children) node.children = walk(children)
            const widgetConfig = WidgetFactory.newWidget(type)
            if (widgetConfig) {
                defaults[type] = widgetConfig.defaults
                widgets[widgetConfig.overrides.id] = {
                    type,
                    ...widgetConfig.overrides,
                    ...rest,
                }
                return { ...node, ...widgetConfig.node }
            }
            return {}
            // console.log('Init state missing widget', type)
        })
    }
    const newTree = walk(Object.assign([], state.tree) as WidgetTreeNode[])
    // console.log('tree init', newTree)
    return {
        key: state.key,
        tree: newTree,
        defaults,
        widgets,
    }
}
