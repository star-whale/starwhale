import React, { useEffect, useMemo } from 'react'
import log from 'loglevel'
import BusyPlaceholder from '@/components/BusyLoaderWrapper/BusyPlaceholder'
import EditorContextProvider from './context/EditorContextProvider'
import { registerWidgets } from './Widget/WidgetFactoryRegister'
import WidgetFactory from './Widget/WidgetFactory'
import { generateId } from './utils/generators'
import { createCustomStore, WidgetTreeNode } from './context/store'
import WidgetRenderTree from './Widget/WidgetRenderTree'
import { EventBusSrv } from './events/events'

// log.enableAll()

export function withEditorRegister(EditorApp: React.FC) {
    return function EditorLoader(props: any) {
        const [registred, setRegistred] = React.useState(false)

        useEffect(() => {
            registerWidgets().then((module) => {
                setRegistred(true)
            })
        }, [])

        if (!registred) {
            return <BusyPlaceholder type='spinner' />
        }
        log.debug('WidgetFactory', WidgetFactory.widgetMap)

        return <EditorApp {...props} />
    }
}

export function witEditorContext(EditorApp: React.FC, rawState: typeof initialState) {
    return function EditorContexted(props: any) {
        const state = useMemo(() => tranformState(rawState), [])
        const value = useMemo(() => {
            const store = createCustomStore(state)
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
                    // children: [],
                },
            ],
        },
        // {
        //     type: 'ui:section',
        //     // children: [],
        // },
    ],
    widgets: {},
    defaults: {},
}
const tranformState = (state: typeof initialState) => {
    const defaults = {} as any
    const widgets = {} as any

    function walk(nodes: WidgetTreeNode[]) {
        return nodes.map((node: WidgetTreeNode) => {
            if (node.children) node.children = walk(node.children)
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
