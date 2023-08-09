import React, { useMemo, useRef } from 'react'
import EditorContextProvider, { StoreType } from '@starwhale/core/context/EditorContextProvider'
import { createCustomStore } from '@starwhale/core/store'
import WidgetRenderTree from '@starwhale/core/widget/WidgetRenderTree'
import { EventBusSrv } from '@starwhale/core/events'
import { useJob } from '@/domain/job/hooks/useJob'
import { tablesOfEvaluation, WidgetStoreState } from '@starwhale/core'
import BusyPlaceholder from '@starwhale/ui/BusyLoaderWrapper/BusyPlaceholder'
import { tranformState } from './utils'
import { useProject } from '@project/hooks/useProject'

export function withProject(EditorApp: React.FC) {
    return function EditorLoader(props: any) {
        const { project } = useProject()
        const projectId = project?.id
        const { job } = useJob()
        // eslint-disable-next-line prefer-template
        const prefix = projectId && job?.uuid ? tablesOfEvaluation(projectId, job?.uuid) + '/' : undefined
        const storeKey = job?.modelName ? ['evaluation-model', job?.modelName].join('-') : undefined
        if (!prefix || !storeKey || !projectId) {
            return <BusyPlaceholder type='spinner' />
        }

        const dynamicVars = { prefix, storeKey, projectId }

        return <EditorApp {...props} dynamicVars={dynamicVars} />
    }
}

const empty = {}

export function witEditorContext(
    EditorApp: React.FC,
    rawState: {
        key: string
        tree: Record<string, any>[]
        widgets: Record<string, any>
        defaults: Record<string, any>
    }
) {
    return function EditorContexted(props: any) {
        const state = useMemo(() => tranformState(rawState), [])
        const store = useRef<StoreType>()
        const value = useMemo(() => {
            if (!store.current) {
                store.current = createCustomStore(state as WidgetStoreState)
            }
            const eventBus = new EventBusSrv()
            return {
                store: store.current,
                eventBus,
            }
        }, [state])

        return (
            <EditorContextProvider
                value={{
                    ...value,
                    dynamicVars: props.dynamicVars || empty,
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
const Editor = withProject(witEditorContext(WidgetRenderTree, initialState))

export { Editor, initialState }
export default Editor
