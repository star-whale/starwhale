import React, { useState } from 'react'
import _ from 'lodash'
import { ErrorBoundary } from '@starwhale/ui'
import { useWidget } from './WidgetFactoryRegister'
import { WidgetRendererProps } from '../types'

const DEBUG = false
export function WidgetRenderer<P extends object = any, F extends object = any>(props: WidgetRendererProps<P, F>) {
    const {
        id,
        type,
        path,
        data,
        optionConfig = {},
        onOptionChange = () => {},
        fieldConfig = {},
        onFieldChange = () => {},
        onLayoutOrderChange = () => {},
        onLayoutChildrenChange = () => {},
        onLayoutCurrentChange = () => {},
        children,
        eventBus,
        ...rest
    } = props

    const { widget } = useWidget(type)
    const [error] = useState<string | undefined>()

    if (error) {
        return <div>Failed to load widget: {error}</div>
    }

    if (!widget) {
        return <div>Loading widget {type}...</div>
    }

    if (!widget.renderer) {
        return <div>Seems like the widget you are trying to load does not have a renderer component.</div>
    }

    // if (!data) {
    //     return <div>No datastore data</div>
    // }

    const WidgetComponent = widget.renderer
    const optionsWithDefaults = _.merge({}, widget.defaults?.optionConfig ?? {}, optionConfig)
    const fieldsWithDefaults = _.merge({}, widget.defaults?.fieldConfig ?? {}, fieldConfig)

    // console.log('WidgetComponent', optionsWithDefaults)

    return (
        <ErrorBoundary>
            {DEBUG && `${type}-${id}`}
            <WidgetComponent
                id={id ?? '0'}
                path={path}
                type={type}
                data={data}
                // title={title}
                // transparent={false}
                // width={width}
                // height={height}
                // renderCounter={0}
                // replaceVariables={(str: string) => str}
                // @ts-ignore
                defaults={widget.defaults ?? {}}
                optionConfig={optionsWithDefaults}
                onOptionChange={onOptionChange}
                //
                fieldConfig={fieldsWithDefaults}
                // @ts-ignore
                onFieldChange={onFieldChange}
                //
                onLayoutOrderChange={onLayoutOrderChange}
                onLayoutChildrenChange={onLayoutChildrenChange}
                onLayoutCurrentChange={onLayoutCurrentChange}
                eventBus={eventBus}
                {...rest}
            >
                {children}
            </WidgetComponent>
        </ErrorBoundary>
    )
}

// export let PanelRenderer: WidgetRendererType = () => {
//     return <div>WidgetRenderer can only be used instance has been started.</div>
// }

// /**
//  * Used to bootstrap the PanelRenderer during application start so the PanelRenderer
//  * is exposed via runtime.
//  *
//  * @internal
//  */
// export function setPanelRenderer(renderer: WidgetRendererType) {
//     PanelRenderer = renderer
// }

export default WidgetRenderer
