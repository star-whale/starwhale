/* eslint-disable */

import React, { useMemo } from 'react'
import log from 'loglevel'
import EditorContextProvider from '@starwhale/core/context/EditorContextProvider'
import { registerWidgets } from '@starwhale/core/widget/WidgetFactoryRegister'
import WidgetFactory from '@starwhale/core/widget/WidgetFactory'
import { createCustomStore, WidgetTreeNode } from '@starwhale/core/store'
import WidgetRenderTree from '@starwhale/core/widget/WidgetRenderTree'
import { EventBusSrv } from '@starwhale/core/events'
import { useProject } from '@/domain/project/hooks/useProject'
import { useJob } from '@/domain/job/hooks/useJob'
import { tablesOfEvaluation } from '@starwhale/core'
import BusyPlaceholder from '../BusyLoaderWrapper/BusyPlaceholder'
import { useParams } from 'react-router'

// log.enableAll()
registerWidgets()

export function withEditorRegister(EditorApp: React.FC) {
    return function EditorLoader(props: any) {
        // const [registred, setRegistred] = React.useState(false)
        // useEffect(() => {
        //     // registerRemoteWidgets().then((module) => {
        //     //     setRegistred(true)
        //     // })
        // }, [])
        // if (!registred) {
        //     return <BusyPlaceholder type='spinner' />
        // }
        log.debug('WidgetFactory', WidgetFactory.widgetMap)
        // @FIXME
        const { projectId, jobId } = useParams<{ projectId: string; jobId: string }>()
        const { project } = useProject()
        const { job } = useJob()
        const prefix = project?.name && job?.uuid ? tablesOfEvaluation(project?.name, job?.uuid) + '/' : undefined
        const storeKey = job?.modelName ? ['evaluation-model', job?.modelName].join('-') : undefined
        if (!prefix || !storeKey || !projectId) {
            return <BusyPlaceholder type='spinner' />
        }

        const dynamicVars = { prefix, storeKey, projectId }

        return <EditorApp {...props} dynamicVars={dynamicVars} />
    }
}

export function witEditorContext(EditorApp: React.FC, rawState: typeof initialState) {
    return function EditorContexted(props: any) {
        // @eslint-disable-next-line typescript-eslint/no-use-before-define
        const state = useMemo(() => tranformState(rawState), [])
        // @NOTICE must only init once
        const value = useMemo(() => {
            // @ts-ignore
            const store = createCustomStore(state)
            const eventBus = new EventBusSrv()
            return {
                store,
                eventBus,
            }
        }, [state])

        return (
            <EditorContextProvider
                value={{
                    ...value,
                    dynamicVars: props.dynamicVars,
                }}
            >
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
            // @ts-ignore
            if (node.children) node.children = walk(node.children)
            const widgetConfig = WidgetFactory.newWidget(node.type)
            if (widgetConfig) {
                defaults[node.type] = widgetConfig.defaults
                widgets[widgetConfig.overrides.id] = widgetConfig.overrides
                return { ...node, ...widgetConfig.node }
            }
            console.log('Init state missing widget', node.type)
            return
        })
    }
    const newTree = walk(Object.assign([], state.tree) as WidgetTreeNode[])
    console.log('tree init', newTree)
    // console.log('INIT TREE', newTree, defaults, widgets)
    return {
        key: state.key,
        tree: newTree,
        defaults,
        widgets,
    }
}

const Editor = withEditorRegister(witEditorContext(WidgetRenderTree, initialState))

export default Editor
