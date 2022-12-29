import React, { useEffect, useMemo } from 'react'
import EditorContextProvider from '@starwhale/core/context/EditorContextProvider'
import { registerWidgets } from '@starwhale/core/widget/WidgetFactoryRegister'
import { createCustomStore } from '@starwhale/core/store'
import WidgetRenderTree from '@starwhale/core/widget/WidgetRenderTree'
import { EventBusSrv } from '@starwhale/core/events'
import { useProject } from '@/domain/project/hooks/useProject'
import { useJob } from '@/domain/job/hooks/useJob'
import { tablesOfEvaluation } from '@starwhale/core'
import { useParams } from 'react-router'
import BusyPlaceholder from '../BusyLoaderWrapper/BusyPlaceholder'
import { tranformState } from './utils'

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
        const [module, setModule] = React.useState<any>(null)
        // }
        useEffect(() => {
            // @ts-ignore
            // eslint-disable-next-line
            import('http://127.0.0.1:8080/widget.js').then((module) => {
                // setRegistred(true)
                console.log(module)
                setModule(module)
            })
        }, [])

        // log.debug('WidgetFactory', WidgetFactory.widgetMap)
        // @FIXME
        const { projectId } = useParams<{ projectId: string }>()
        const { project } = useProject()
        const { job } = useJob()
        // eslint-disable-next-line prefer-template
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
const Editor = withEditorRegister(witEditorContext(WidgetRenderTree, initialState))

export default Editor
