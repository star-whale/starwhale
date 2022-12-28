import React, { useCallback, useEffect, useRef, useState } from 'react'
import { Subscription } from 'rxjs'
import useSelector, { getWidget } from '../store/hooks/useSelector'
import { useEditorContext } from '../context/EditorContextProvider'
import { WidgetRendererType } from '../types'
import { useQueryDatastore } from '../datastore/hooks/useFetchDatastore'
import { useIsInViewport } from '../utils'
import { exportTable } from '../datastore'
import { PanelDownloadEvent, PanelReloadEvent } from '../events'

function getParentPath(paths: any[]) {
    const curr = paths.slice()
    const parentIndex = paths.lastIndexOf('children')
    return curr.slice(0, parentIndex + 1)
}

function getChildrenPath(paths: any[]) {
    return [...paths, 'children']
}

export default function withWidgetDynamicProps(WrappedWidgetRender: WidgetRendererType) {
    function WrapedPropsWidget(props: any) {
        const { id, path } = props
        const { store, eventBus } = useEditorContext()
        const api = store()
        const overrides = useSelector(getWidget(id)) ?? {}

        const handleLayoutOrderChange = useCallback(
            (newList) => {
                const paths = ['tree', ...path, 'children']
                api.onLayoutOrderChange(paths, newList)
            },
            [api, path]
        )
        const handleLayoutChildrenChange = useCallback(
            (widget: any, payload: Record<string, any>) => {
                const paths = ['tree', ...path, 'children']
                api.onLayoutChildrenChange(paths, getChildrenPath(paths), widget, payload)
            },
            [api, path]
        )
        const handleLayoutCurrentChange = useCallback(
            (widget: any, payload: Record<string, any>) => {
                // @FIXME path utils
                const paths = ['tree', ...path]
                api.onLayoutChildrenChange(paths, getParentPath(paths), widget, payload)
            },
            [api, path]
        )

        // @FIXME show datastore be fetch at here
        // @FIXME refrech setting
        const tableName = overrides?.fieldConfig?.data?.tableName

        const query = React.useMemo(
            () => ({
                tableName,
                start: 0,
                limit: 1000,
                rawResult: true,
                ignoreNonExistingTable: true,
            }),
            [tableName]
        )

        const myRef = useRef<HTMLElement>()
        const inViewport = useIsInViewport(myRef as any)
        const info = useQueryDatastore(query, false)
        const [loaded, setLoaded] = useState(false)

        //console.log(inViewport, loaded, info, tableName)

        useEffect(() => {
            if (tableName && inViewport && !loaded) {
                info.refetch()
                setLoaded(true)
            }
            // eslint-disable-next-line react-hooks/exhaustive-deps
        }, [tableName, inViewport, loaded])

        useEffect(() => {
            // @FIXME better use scoped eventBus
            const subscription = new Subscription()
            subscription.add(
                eventBus.getStream(PanelDownloadEvent).subscribe({
                    next: (evt) => {
                        if (evt.payload?.id === id) {
                            exportTable(query)
                        }
                    },
                })
            )
            subscription.add(
                eventBus.getStream(PanelReloadEvent).subscribe({
                    next: async (evt) => {
                        if (evt.payload?.id === id) {
                            info.refetch()
                        }
                    },
                })
            )
            return () => subscription.unsubscribe()
        }, [eventBus, id, info, query])

        return (
            <div ref={myRef as any} style={{ width: '100%', height: '100%' }}>
                <WrappedWidgetRender
                    {...props}
                    name={overrides.name}
                    data={info?.data}
                    optionConfig={overrides.optionConfig}
                    onOptionChange={(config) => api.onConfigChange(['widgets', id, 'optionConfig'], config)}
                    fieldConfig={overrides.fieldConfig}
                    onFieldChange={(config) => api.onConfigChange(['widgets', id, 'fieldConfig'], config)}
                    onLayoutOrderChange={handleLayoutOrderChange}
                    onLayoutChildrenChange={handleLayoutChildrenChange}
                    onLayoutCurrentChange={handleLayoutCurrentChange}
                    onDataReload={() => info.refetch()}
                    onDataDownload={() => exportTable(query)}
                    eventBus={eventBus}
                />
            </div>
        )
    }
    return WrapedPropsWidget
}
