/* eslint-disable */
/* @ts-nocheck */
import create from 'zustand'
import { devtools, subscribeWithSelector, persist } from 'zustand/middleware'
import produce from 'immer'
import { arrayMove, arrayRemove } from 'react-movable'
import _ from 'lodash'
import WidgetFactory, { WidgetConfig, WidgetConfigProps } from '../Widget/WidgetFactory'
import { getTreePath } from '../utils/path'

export type WidgetType = string

export type LayoutWidget = ''
export type WidgetLayoutType = {
    dndList: 'dndList'
}

export type WidgetTreeNode = {
    id?: string
    type: string
    children?: WidgetTreeNode[]
}
export type WidgetStoreState = {
    key: string
    time: number
    tree: WidgetTreeNode[]
    widgets: Record<string, any>
    defaults: Record<string, any>
    onOrderChange: any
    onConfigChange: any
    onLayoutChildrenChange: any
    onWidgetChange: any
}

// const transformWidget = (node) => {
//     // console.log(node)
//     if (!node?.id) node.id = generateId(node.type)
//     const defaultConfig = WidgetFactory.widgetConfigMap.get(node.type) ?? {}
//     const config = WidgetFactory.widgetConfigMap.get(node.type) ?? {}
//     return {
//         node,
//         defaultConfig,
//         config,
//     }
// }

export function createCustomStore(initState: Partial<WidgetStoreState> = {}) {
    console.log('store init')
    const name = 'widgets'
    const useStore = create<WidgetStoreState>()(
        subscribeWithSelector(
            devtools(
                // persist(
                (set, get, store) => ({
                    ...(initState as any),
                    key: name,
                    time: 0,
                    onLayoutOrderChange: (paths: any, oldIndex, newIndex) =>
                        set(
                            produce((state) => {
                                const nodes = _.get(get(), paths)
                                console.log(get(), nodes, paths)
                                const ordered =
                                    newIndex === -1
                                        ? arrayRemove(nodes, oldIndex)
                                        : arrayMove(nodes, oldIndex, newIndex)
                                _.set(state, paths, ordered)
                            })
                        ),
                    onConfigChange: (paths: any, config: any) =>
                        set(
                            produce((state) => {
                                const rawConfig = _.get(get(), paths) ?? {}
                                _.set(state, paths, _.merge({}, rawConfig, config))
                                console.log('onConfigChange', state, paths, rawConfig, config)
                            })
                        ),
                    onWidgetChange: (id: string, widgets: WidgetConfigProps) =>
                        set(
                            produce((state) => {
                                const { type } = widgets ?? {}
                                const { type: currType } = state.widgets?.[id] ?? {}
                                if (type != currType) {
                                    const { current } = getTreePath(state, id)
                                    const node = _.get(state, current)

                                    console.log('---', id, node, type, currType, current)

                                    // udpate tree ndoe
                                    _.set(state, current, {
                                        ...node,
                                        type,
                                    })

                                    // update defaults
                                    const config = WidgetFactory.newWidget(type)
                                    if (!config) return
                                    const { defaults, overrides } = config
                                    if (!state.defaults[type]) state.defaults[type] = defaults
                                }

                                state.widgets[id] = _.merge({}, state.widgets?.[id], widgets)
                            })
                        ),
                    onLayoutChildrenChange: (
                        paths: any[],
                        sourcePaths: any[],
                        widgets: WidgetConfig,
                        payload: any = { type: 'append' }
                    ) =>
                        set(
                            produce((state) => {
                                const { type } = widgets
                                const currentIndex = getCurrentIndex(paths)
                                const curr = _.get(get(), sourcePaths) ?? []
                                //
                                if (payload.type === 'delete') {
                                    const darr = curr.slice()
                                    darr.splice(currentIndex, 1)
                                    _.set(state, sourcePaths, darr)
                                    delete state.widgets[payload.id]
                                    return
                                }
                                //
                                const config = WidgetFactory.newWidget(type)
                                if (!config) return
                                const { defaults, overrides, node } = config
                                state.widgets[overrides.id] = { ...widgets, ...overrides }
                                state.defaults[type] = defaults

                                // @FIXME abstract replace/add/....
                                switch (payload.type) {
                                    case 'append':
                                        _.set(state, sourcePaths, [...curr, node])
                                        break
                                    case 'addAbove':
                                        const arr = curr.slice()
                                        arr.splice(currentIndex, 0, node)
                                        _.set(state, sourcePaths, arr)
                                        break
                                    case 'addBelow':
                                        const arr2 = curr.slice()
                                        arr2.splice(currentIndex + 1, 0, node)
                                        _.set(state, sourcePaths, arr2)
                                        break
                                }
                            })
                        ),
                }),
                { name: initState.key ?? name }
                // ),
                // { name: initState.key ?? name }
            )
        )
    )
    // eslint-disable-next-line
    // useStore.subscribe(console.log)
    // @ts-ignore
    return useStore
}
export default {
    createCustomStore,
}

function getCurrentIndex(paths: any[]) {
    return paths[paths.length - 1]
}
