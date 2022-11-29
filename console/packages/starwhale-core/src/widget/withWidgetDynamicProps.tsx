import React, { useCallback, useEffect, useRef } from 'react'
import { useQueryDatastore } from '@/domain/datastore/hooks/useFetchDatastore'
import useSelector, { getWidget } from '../store/hooks/useSelector'
import { useEditorContext } from '../context/EditorContextProvider'
import { WidgetRendererType } from '../types'
import WidgetModel from './WidgetModel'

export default function withWidgetDynamicProps(WrappedWidgetRender: WidgetRendererType) {
    function WrapedPropsWidget(props: any) {
        const { id, type, path } = props
        const { store, eventBus, dynamicVars } = useEditorContext()
        const api = store()
        const overrides = useSelector(getWidget(id)) ?? {}

        // const model = useRef(new WidgetModel({ id, type }))
        // useEffect(() => {
        //     model.current.setDynamicVars(dynamicVars)
        //     model.current.setOverrides(overrides)
        // }, [overrides, dynamicVars])

        // console.log('【model】', model.current.type, model.current.id, model)

        const handleLayoutOrderChange = useCallback(
            (oldIndex, newIndex) => {
                const paths = ['tree', ...path, 'children']
                api.onLayoutOrderChange(paths, oldIndex, newIndex)
            },
            [api]
        )
        const handleLayoutChildrenChange = useCallback(
            (widget: any, payload: Record<string, any>) => {
                const paths = ['tree', ...path, 'children']
                api.onLayoutChildrenChange(paths, getChildrenPath(paths), widget, payload)
            },
            [api]
        )
        const handleLayoutCurrentChange = useCallback(
            (widget: any, payload: Record<string, any>) => {
                // @FIXME path utils
                const paths = ['tree', ...path]
                api.onLayoutChildrenChange(paths, getParentPath(paths), widget, payload)
            },
            [api]
        )

        // @FIXME show datastore be fetch at here
        // @FIXME refrech setting
        const tableName = overrides?.fieldConfig?.data?.tableName

        const query = React.useMemo(
            () => ({
                tableName,
                start: 0,
                limit: 99999,
                rawResult: true,
                ignoreNonExistingTable: true,
                // filter,
            }),
            [tableName]
        )

        const info = useQueryDatastore(query, false)

        useEffect(() => {
            if (tableName) info.refetch()
        }, [tableName, type])

        // console.log(tableName)

        return (
            <WrappedWidgetRender
                {...props}
                name={overrides.name}
                data={info?.data}
                optionConfig={overrides.optionConfig}
                onOptionChange={(config) => api.onConfigChange(['widgets', id, 'optionConfig'], config)}
                // onOptionChange={(config) => {
                //     model.current.updateOptionConfig(config)
                //     model.current.saveToStore(api)
                // }}
                fieldConfig={overrides.fieldConfig}
                onFieldChange={(config) => api.onConfigChange(['widgets', id, 'fieldConfig'], config)}
                // onFieldChange={(config) => {
                //     model.current.updateFieldConfig(config)
                //     model.current.saveToStore(api)
                // }}
                // for layout
                onLayoutOrderChange={handleLayoutOrderChange}
                onLayoutChildrenChange={handleLayoutChildrenChange}
                onLayoutCurrentChange={handleLayoutCurrentChange}
                eventBus={eventBus}
                // for
            />
        )
    }
    return WrapedPropsWidget
}

function getParentPath(paths: any[]) {
    const curr = paths.slice()
    const parentIndex = paths.lastIndexOf('children')
    return curr.slice(0, parentIndex + 1)
}

function getChildrenPath(paths: any[]) {
    return [...paths, 'children']
}
