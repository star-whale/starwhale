import React, { useCallback, useEffect, useRef } from 'react'
import { Subscription } from 'rxjs'
import { getWidget } from '../store/hooks/useSelector'
import { useEditorContext } from '../context/EditorContextProvider'
import { WidgetRendererType, WidgetStoreState } from '../types'
import useFetchDatastoreByTable from '../datastore/hooks/useFetchDatastoreByTable'

import { useIsInViewport } from '../utils'
import { exportTable } from '../datastore'
import { PanelDownloadEvent, PanelReloadEvent } from '../events'
import { BusyPlaceholder } from '@starwhale/ui/BusyLoaderWrapper'
import shallow from 'zustand/shallow'

function getParentPath(paths: any[]) {
    const curr = paths.slice()
    const parentIndex = paths.lastIndexOf('children')
    return curr.slice(0, parentIndex + 1)
}

function getChildrenPath(paths: any[]) {
    return [...paths, 'children']
}
const selector = (s: WidgetStoreState) => ({
    onLayoutChildrenChange: s.onLayoutChildrenChange,
    onLayoutOrderChange: s.onLayoutOrderChange,
    onConfigChange: s.onConfigChange,
})

export default function withWidgetDynamicProps(WrappedWidgetRender: WidgetRendererType) {
    function WrapedPropsWidget(props: any) {
        const { id, path } = props
        const { store, eventBus } = useEditorContext()
        const api = store(selector, shallow)
        const widgetIdSelector = React.useMemo(() => getWidget(id) ?? {}, [id])
        const overrides = store(widgetIdSelector)
        const myRef = useRef<HTMLElement>()

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
        const tableName = React.useMemo(() => overrides?.fieldConfig?.data?.tableName, [overrides])
        const tableConfig = React.useMemo(() => overrides?.optionConfig?.currentView ?? {}, [overrides])
        const tableOptions = React.useMemo(() => {
            const sorts = tableConfig.sortBy
                ? [
                      {
                          columnName: tableConfig.sortBy,
                          descending: tableConfig.sortDirection === 'DESC',
                      },
                  ]
                : []

            sorts.push({
                columnName: 'id',
                descending: true,
            })

            return {
                pageNum: 1,
                pageSize: 1000,
                query: {
                    orderBy: sorts,
                },
                filter: tableConfig.queries,
            }
        }, [tableConfig])
        const inViewport = useIsInViewport(myRef as any)
        const { recordInfo, recordQuery: query } = useFetchDatastoreByTable(tableName, tableOptions, false)
        const inViewLoadRef = useRef(false)
        const tableNameRef = useRef('')

        // if in viewport, refetch data
        // if panel table changed, refetch data
        useEffect(() => {
            if (!tableName || !inViewport) return

            if (tableNameRef.current !== tableName) {
                recordInfo.refetch()
                tableNameRef.current = tableName
                return
            }

            if (inViewLoadRef.current) return
            recordInfo.refetch()

            inViewLoadRef.current = true
            tableNameRef.current = tableName
            // eslint-disable-next-line react-hooks/exhaustive-deps
        }, [tableName, inViewport])

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
                            recordInfo.refetch()
                        }
                    },
                })
            )
            return () => subscription.unsubscribe()
            // eslint-disable-next-line react-hooks/exhaustive-deps
        }, [eventBus, id, query])

        if (tableName && !recordInfo.isSuccess)
            return (
                <div ref={myRef as any} style={{ width: '100%', height: '100%' }}>
                    <BusyPlaceholder style={{ minHeight: 'auto' }} />
                </div>
            )

        return (
            <div
                ref={myRef as any}
                style={{
                    width: '100%',
                    height: '100%',
                }}
            >
                <WrappedWidgetRender
                    {...props}
                    name={overrides?.name}
                    data={recordInfo?.data}
                    optionConfig={overrides?.optionConfig}
                    onOptionChange={(config) => api.onConfigChange(['widgets', id, 'optionConfig'], config)}
                    fieldConfig={overrides?.fieldConfig}
                    onFieldChange={(config) => api.onConfigChange(['widgets', id, 'fieldConfig'], config)}
                    onLayoutOrderChange={handleLayoutOrderChange}
                    onLayoutChildrenChange={handleLayoutChildrenChange}
                    onLayoutCurrentChange={handleLayoutCurrentChange}
                    onDataReload={() => recordInfo.refetch()}
                    onDataDownload={() => exportTable(query)}
                    eventBus={eventBus}
                />
            </div>
        )
    }
    return WrapedPropsWidget
}
