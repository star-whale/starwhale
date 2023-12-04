import React, { useCallback, useEffect, useRef } from 'react'
import { Subscription } from 'rxjs'
import { getWidget } from '../store/hooks/useSelector'
import { useEditorContext } from '../context/EditorContextProvider'
import { WidgetRendererType, WidgetStoreState } from '../types'
import useFetchDatastoreByTable from '../datastore/hooks/useFetchDatastoreByTable'

import { useEventCallback, useIsInViewport } from '../utils'
import { exportTable } from '../datastore'
import { PanelChartDownloadEvent, PanelChartReloadEvent } from '../events'
import { BusyPlaceholder } from '@starwhale/ui/BusyLoaderWrapper'
import { shallow } from 'zustand/shallow'
import useDatastorePage from '../datastore/hooks/useDatastorePage'
import { usePanelDatastore } from '../context'

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
    isEditable: s.isEditable,
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
        const handleOptionConfigChange = useEventCallback((config) =>
            api.onConfigChange(['widgets', id, 'optionConfig'], config)
        )
        const handleFieldConfigChange = useEventCallback((config) =>
            api.onConfigChange(['widgets', id, 'fieldConfig'], config)
        )

        const { getPrefixes } = usePanelDatastore()
        const prefixes = React.useMemo(() => getPrefixes(), [getPrefixes])

        // @FIXME show datastore be fetch at here
        // @FIXME refrech setting
        const tableName = React.useMemo(() => overrides?.fieldConfig?.data?.tableName, [overrides])
        const tableConfig = React.useMemo(() => overrides?.optionConfig?.currentView, [overrides])

        const { page, setPage, params } = useDatastorePage({
            pageNum: 1,
            pageSize: 50,
            sortBy: tableConfig?.sortBy || 'id',
            sortDirection: tableConfig?.sortDirection || 'DESC',
            queries: tableConfig?.queries,
            tableName,
            prefixFn: React.useCallback(
                (tname: string) => {
                    const p = prefixes?.find((item: any) => tname.startsWith(item.name))?.prefix
                    return p || ''
                },
                [prefixes]
            ),
        })

        const inViewport = useIsInViewport(myRef as any)
        const [enableLoad, setEnableload] = React.useState(false)
        const {
            lastKey,
            recordInfo,
            recordQuery: query,
            columnTypes,
            records,
            getTableRecordMap,
            getTableColumnTypeMap,
        } = useFetchDatastoreByTable(params, enableLoad)

        useEffect(() => {
            if (enableLoad) return
            if (inViewport) setEnableload(true)
        }, [inViewport, enableLoad])

        useEffect(() => {
            // @FIXME better use scoped eventBus
            const subscription = new Subscription()
            subscription.add(
                eventBus.getStream(PanelChartDownloadEvent).subscribe({
                    next: (evt) => {
                        if (evt.payload?.id === id && query) {
                            exportTable({ ...query, encodeWithType: false, limit: -1 })
                        }
                    },
                })
            )
            subscription.add(
                eventBus.getStream(PanelChartReloadEvent).subscribe({
                    next: async (evt) => {
                        if (evt.payload?.id === id && query) {
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
                recordQuery: query,
                records,
                columnTypes,
                getTableRecordMap,
                getTableColumnTypeMap,
            }
        }, [recordInfo.isSuccess, query, records, columnTypes, getTableRecordMap, getTableColumnTypeMap])

        // useIfChanged({
        //     ...props,
        //     getPrefixes,
        //     path: props.path,
        //     data: $data,
        //     query,
        //     records,
        //     columnTypes,
        //     getTableRecordMap,
        //     getTableColumnTypeMap,
        //     params,
        //     prefixes,
        //     overrides,
        //     optionConfig: overrides.optionConfig,
        //     evalSelectData: overrides.optionConfig?.evalSelectData,
        // })

        const handleDataReload = useEventCallback(() => query && recordInfo.refetch())
        const handleDataDownload = useEventCallback(
            () => query && exportTable({ ...query, encodeWithType: false, limit: -1 })
        )
        const handlePageChange = useEventCallback((tmp: any) => setPage(tmp, lastKey))

        return (
            <div
                ref={myRef as any}
                style={{
                    width: '100%',
                    height: '100%',
                }}
            >
                {tableName && !recordInfo.isSuccess ? (
                    <BusyPlaceholder style={{ minHeight: 'auto' }} type={recordInfo.isLoading ? 'spinner' : 'empty'} />
                ) : (
                    <WrappedWidgetRender
                        {...props}
                        readonly={!api.isEditable()}
                        name={overrides?.name}
                        data={$data}
                        page={page}
                        onPageChange={handlePageChange}
                        optionConfig={overrides?.optionConfig}
                        onOptionChange={handleOptionConfigChange}
                        fieldConfig={overrides?.fieldConfig}
                        onFieldChange={handleFieldConfigChange}
                        onLayoutOrderChange={handleLayoutOrderChange}
                        onLayoutChildrenChange={handleLayoutChildrenChange}
                        onLayoutCurrentChange={handleLayoutCurrentChange}
                        onDataReload={handleDataReload}
                        onDataDownload={handleDataDownload}
                        eventBus={eventBus}
                    />
                )}
            </div>
        )
    }
    return WrapedPropsWidget
}
