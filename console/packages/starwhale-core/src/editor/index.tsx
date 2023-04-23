/* eslint-disable */
import React, { useEffect, useMemo } from 'react'
import log from 'loglevel'
import EditorContextProvider from '../context/EditorContextProvider'
import { registerWidgets } from '../widget/WidgetFactoryRegister'
import WidgetFactory from '../widget/WidgetFactory'
import { createCustomStore } from '../store/store'
import WidgetRenderTree from '../widget/WidgetRenderTree'
import { EventBusSrv } from '../events/events'
import { WidgetTreeNode } from '../types'

// export const registerRemoteWidgets = async () => {
//     // @FIXME store module meta from backend
//     // meta was defined by system not user
//     const start = performance.now()

//     // must be remote component that packaged
//     const modules = [
//         { type: 'ui:dndList', url: '../widgets/DNDListWidget/index.tsx' },
//         { type: 'ui:section', url: '../widgets/SectionWidget/index.tsx' },
//         { type: 'ui:panel:table', url: '../widgets/PanelTableWidget/index.tsx' },
//         { type: 'ui:panel:rocauc', url: '../widgets/PanelRocAucWidget/index.tsx' },
//         { type: 'ui:panel:heatmap', url: '../widgets/PanelHeatmapWidget/index.tsx' },
//     ].filter((v) => !(v.type in WidgetFactory.widgetTypes))

//     /* @vite-ignore */
//     for await (const module of modules.map(async (m) => import(m.url))) {
//         const widget = module.default as WidgetPlugin
//         registerWidget(widget, widget.defaults)
//     }

//     console.log('Widget registration took: ', performance.now() - start, 'ms')
// }
// log.enableAll()

export function withEditorRegister(EditorApp: React.FC) {
    registerWidgets()

    return function EditorLoader(props: any) {
        // const [registred, setRegistred] = React.useState(false)
        useEffect(() => {
            /*
            import('http://127.0.0.1:8080/widget.js').then((module) => {
                // setRegistred(true)
                console.log(module)
            })
            */
        }, [])
        // if (!registred) {
        //     return <BusyPlaceholder type='spinner' />
        // }
        log.debug('WidgetFactory', WidgetFactory.widgetMap)

        return <EditorApp {...props} />
    }
}

export function witEditorContext(EditorApp: React.FC, rawState: typeof initialState) {
    return function EditorContexted(props: any) {
        const state = useMemo(() => tranformState(rawState), [])
        const value = useMemo(() => {
            const store = createCustomStore(state as any)
            const eventBus = new EventBusSrv()
            log.debug('store', state)
            return {
                store,
                eventBus,
            }
        }, [state])

        return (
            <EditorContextProvider value={value}>
                <EditorApp {...props} />
            </EditorContextProvider>
        )
    }
}

const initialState = {
    key: 'widgets',
    tree: [
        {
            type: 'ui:dndList',
            children: [
                {
                    type: 'ui:section',
                },
            ],
        },
    ],
    widgets: {},
    defaults: {},
}
const tranformState = (state: typeof initialState) => {
    const defaults = {} as any
    const widgets = {} as any

    function walk(nodes: WidgetTreeNode[]) {
        return nodes.map((node: WidgetTreeNode) => {
            if (node.children) node.children = walk(node.children) as any
            const widgetConfig = WidgetFactory.newWidget(node.type)
            if (widgetConfig) {
                defaults[node.type] = widgetConfig.defaults
                widgets[widgetConfig.overrides.id] = widgetConfig.overrides
                return { ...node, ...widgetConfig.node }
            }
            console.log('Init state missing widget', node.type)
        })
    }
    const newTree = walk(Object.assign([], state.tree) as WidgetTreeNode[])
    console.log('INIT TREE', newTree, defaults, widgets)
    return {
        key: state.key,
        tree: newTree,
        defaults,
        widgets,
    }
}

const Editor = withEditorRegister(witEditorContext(WidgetRenderTree, initialState))

export default Editor
