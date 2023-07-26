import { WidgetGroupType, WidgetType } from '../types'
import WidgetPlugin from './WidgetPlugin'
import { generateId } from '../utils/generators'
import modules from './WidgetModules'

export type DerivedPropertiesMap = Record<string, string>

class WidgetFactory {
    static widgetTypes: Record<string, string> = {}

    static widgetMap: Map<WidgetType, WidgetPlugin> = new Map()

    static register(widgetType: string, widget: WidgetPlugin) {
        if (!this.widgetTypes[widgetType]) {
            this.widgetTypes[widgetType] = widgetType
            this.widgetMap.set(widgetType, widget)
        }
    }

    static hasWidget(widgetType: WidgetType) {
        return this.widgetMap.has(widgetType)
    }

    static getWidgetTypes(): WidgetType[] {
        return Array.from(this.widgetMap.keys())
    }

    static getPanels() {
        return Array.from(this.widgetMap.values())
            .filter((plugin) => plugin.defaults?.group === WidgetGroupType.PANEL)
            .map((plugin) => plugin.defaults)
    }

    static getWidget(widgetType: WidgetType) {
        if (!this.widgetTypes[widgetType]) return undefined
        return this.widgetMap.get(widgetType)
    }

    static newWidget(widgetType: WidgetType) {
        if (!this.widgetMap.has(widgetType)) return undefined
        const widget = this.widgetMap.get(widgetType) as WidgetPlugin
        const id = generateId(widget.defaults?.group ?? '')

        return {
            defaults: widget.defaults,
            overrides: { id },
            node: {
                type: widgetType,
                id,
            },
        }
    }
}

modules.forEach((w: WidgetPlugin<any>) => {
    if (!w.getType()) return
    if (WidgetFactory.hasWidget(w.getType())) return
    WidgetFactory.register(w.getType(), w)
})

export default WidgetFactory

// @ts-ignore
if (import.meta.hot) {
    // @ts-ignore
    import.meta.hot.accept(() => {
        // eslint-disable-next-line no-console
        console.log('hot reload widget modules')
        modules.forEach((w: WidgetPlugin<any>) => {
            WidgetFactory.register(w.getType(), w)
        })
    })
}
