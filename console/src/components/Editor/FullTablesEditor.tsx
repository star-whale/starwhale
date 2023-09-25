import React, { useMemo, useRef } from 'react'
import EditorContextProvider, { StoreType } from '@starwhale/core/context/EditorContextProvider'
import { createCustomStore } from '@starwhale/core/store'
import WidgetRenderTree from '@starwhale/core/widget/WidgetRenderTree'
import { EventBusSrv } from '@starwhale/core/events'
import { useFetchDatastoreAllTables, WidgetStoreState } from '@starwhale/core'
import BusyPlaceholder from '@starwhale/ui/BusyLoaderWrapper/BusyPlaceholder'
import { tranformState } from './utils'
import { withProject } from './Editor'
import { withDefaultWidgets } from '@starwhale/core/widget'
import StoreUpdater from '@starwhale/core/store/StoreUpdater'

function groupBy(names: string[]) {
    const m = {}
    names?.forEach((name) => {
        const key = name.split('/')?.[5]
        // group by arr the fifth element
        m[key] = m[key] || []
        m[key].push(name)
    })
    return m
}

function withEditorContext<EditorAppPropsT>(EditorApp: React.FC<EditorAppPropsT>) {
    return function EditorContexted(props: EditorAppPropsT & { dynamicVars?: any } & any) {
        const { prefix } = props.dynamicVars
        const { isLoading, isSuccess, names, tables } = useFetchDatastoreAllTables(prefix)
        const store = useRef<StoreType>()
        const state = useMemo(() => {
            const group: [string, string[]][] = Object.entries(groupBy(names))

            return tranformState({
                key: 'widgets',
                tree: [
                    {
                        type: 'ui:dndList',
                        children:
                            group?.map(([key, values], index) => {
                                return {
                                    type: 'ui:section',
                                    optionConfig: {
                                        layout: {
                                            width: 600,
                                            height: 500,
                                        },
                                        title: key,
                                        isExpaned: index < 2,
                                    },
                                    children: values?.map((name) => {
                                        return {
                                            type: 'ui:panel:table',
                                            fieldConfig: {
                                                data: {
                                                    chartType: 'ui:panel:table',
                                                    tableName: name,
                                                },
                                            },
                                        }
                                    }),
                                }
                            }) || [],
                    },
                ],
                widgets: {},
                defaults: {},
            })
        }, [names])

        const value = useMemo(() => {
            if (!isSuccess) return undefined

            if (!store.current) {
                store.current = createCustomStore(state as WidgetStoreState)
            } else {
                store.current.setState({
                    ...(state as WidgetStoreState),
                })
            }
            const eventBus = new EventBusSrv()
            return {
                store: store.current,
                eventBus,
            }
        }, [isSuccess, state])

        if (isLoading) {
            return <BusyPlaceholder type='spinner' />
        }

        if (!value) {
            return <BusyPlaceholder type='empty' />
        }

        return (
            <EditorContextProvider
                value={{
                    ...value,
                    tables,
                    dynamicVars: props.dynamicVars,
                }}
            >
                <StoreUpdater {...props} />
                <EditorApp {...props} />
            </EditorContextProvider>
        )
    }
}

const FullTablesEditor = withProject(withDefaultWidgets(withEditorContext(WidgetRenderTree)))

export { FullTablesEditor }
export default FullTablesEditor
