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
import useDatastorePage from '../datastore/hooks/useDatastorePage'

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
        const tableConfig = React.useMemo(() => overrides?.optionConfig?.currentView, [overrides])
        const { page, setPage, getQueryParams } = useDatastorePage({
            pageNum: 1,
            pageSize: 1000,
            sortBy: tableConfig?.sortBy || 'id',
            sortDirection: tableConfig?.sortDirection || 'DESC',
            queries: tableConfig?.queries,
        })
        const inViewport = useIsInViewport(myRef as any)
        const [enableLoad, setEnableload] = React.useState(false)
        const {
            recordInfo,
            recordQuery: query,
            columnTypes,
            records,
        } = useFetchDatastoreByTable(getQueryParams(tableName), enableLoad)
        useEffect(() => {
            if (enableLoad) return
            if (inViewport) setEnableload(true)
        }, [inViewport, enableLoad])

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

        const $data = React.useMemo(() => {
            if (!recordInfo.isSuccess) return { records: [], columnTypes: [] }
            return {
                records,
                columnTypes,
            }
        }, [recordInfo.isSuccess, records, columnTypes])

        return (
            <div
                ref={myRef as any}
                style={{
                    width: '100%',
                    height: '100%',
                }}
            >
                {tableName && !recordInfo.isSuccess ? (
                    <BusyPlaceholder style={{ minHeight: 'auto' }} />
                ) : (
                    <WrappedWidgetRender
                        {...props}
                        name={overrides?.name}
                        data={$data}
                        page={page}
                        onPageChange={setPage}
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
                )}
            </div>
        )
    }
    return WrapedPropsWidget
}
