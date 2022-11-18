import React from 'react'
import { EditorContext } from '../context/EditorContextProvider'
import { WidgetComponent, WidgetConfig, WidgetFieldConfig, WidgetMeta } from './const'

export type WidgetState = Record<string, unknown>

class BaseWidget<T extends WidgetMeta = WidgetMeta> {
    static contextType = EditorContext

    declare context: React.ContextType<typeof EditorContext>

    meta?: T
}

class WidgetPlugin<
    O extends object = any,
    S extends WidgetFieldConfig = WidgetFieldConfig
> extends BaseWidget<WidgetMeta> {
    renderer: WidgetComponent<O, S> | null

    defaults: WidgetConfig<O, S>

    constructor(renderer: WidgetComponent<O, S>, config: WidgetConfig<O, S>) {
        super()
        this.renderer = renderer
        this.defaults = config
    }

    // addConfig(config: any) {
    //     this._defaults = config
    //     return this
    // }

    // get defaults() {
    //     return this._defaults
    // }

    // get type() {
    //     return this._defaults.type
    // }
}

export default WidgetPlugin
