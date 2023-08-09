/* eslint-disable */
/* @ts-nocheck */
import create from 'zustand'
import { devtools, subscribeWithSelector } from 'zustand/middleware'
import produce from 'immer'
import _ from 'lodash'
import WidgetFactory from '../widget/WidgetFactory'
import { getTreePath } from '../utils/path'
import { WidgetConfig, WidgetStoreState, WidgetTreeNode } from '../types'

function arrayOverride(objValue: any, srcValue: any) {
    if (_.isArray(objValue)) {
        return srcValue
    }
}

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
                    onLayoutOrderChange: (paths: any, newOrderList: { id: string }[]) =>
                        set(
                            produce((state) => {
                                const nodes = _.get(get(), paths)
                                // console.log(get(), nodes, paths)

                                const ordered = newOrderList
                                    .map((item) => nodes.find((v: any) => v?.id === item.id))
                                    .filter((v: any) => !!v)
                                _.set(state, paths, ordered)
                            })
                        ),
                    onConfigChange: (paths: any, config: any) =>
                        set(
                            produce((state) => {
                                const rawConfig = _.merge({}, _.get(get(), paths))
                                _.set(state, paths, _.mergeWith(rawConfig, config, arrayOverride))
                                // console.log('onConfigChange', get(), paths, config)
                            })
                        ),
                    onWidgetChange: (id: string, widgets: WidgetConfig) =>
                        set(
                            produce((state) => {
                                const { type } = widgets ?? {}
                                const { type: currType } = state.widgets?.[id] ?? {}
                                if (type != currType) {
                                    const { current } = getTreePath(state, id)
                                    const node = _.get(state, current)

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

                                const rawConfig = _.merge({}, state.widgets?.[id])
                                state.widgets[id] = _.mergeWith(rawConfig, widgets, arrayOverride)
                            })
                        ),
                    onWidgetDelete: (id: string) =>
                        set(
                            produce((state) => {
                                const { type } = _.get(get(), ['widgets', id], {})
                                if (!id || !type) return
                                const { current, parent } = getTreePath(state, id)
                                const currentIndex = getCurrentIndex(current)
                                const currentParent = _.get(state, parent) ?? []
                                const darr = currentParent.slice()
                                darr.splice(currentIndex, 1)
                                _.set(state, parent, darr)
                                delete state.widgets[id]
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
                                    const d = darr.splice(currentIndex, 1)
                                    _.set(state, sourcePaths, darr)
                                    const deepDelete = (node: WidgetTreeNode) => {
                                        if (!node) {
                                            return
                                        }
                                        if (node.id) {
                                            delete state.widgets[node.id]
                                        }
                                        if (node.children) {
                                            node.children.forEach((i) => deepDelete(i))
                                        }
                                    }
                                    deepDelete(d[0])
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
