import { WidgetGroupType, WidgetType } from '../types'
import WidgetPlugin from './WidgetPlugin'
import { generateId } from '../utils/generators'
import WIDGETS from './WidgetModules'
import _ from 'lodash'
import { useEffect, useState } from 'react'

export type DerivedPropertiesMap = Record<string, string>

class WidgetFactory {
    static widgetTypes: Record<string, string> = {}

    static widgetMap: Map<WidgetType, WidgetPlugin> = new Map()

    static panelGroup = WidgetGroupType.PANEL

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

    static getPanels(panelGroup = WidgetGroupType.PANEL) {
        return Array.from(this.widgetMap.values())
            .filter((plugin) => {
                if (_.isString(plugin.defaults?.group))
                    return plugin.defaults?.group === panelGroup || plugin.defaults?.group.includes(WidgetGroupType.ALL)
                if (_.isArray(plugin.defaults?.group))
                    return (
                        plugin.defaults?.group.includes(panelGroup) ||
                        plugin.defaults?.group.includes(WidgetGroupType.ALL)
                    )
                return false
            })
            .map((plugin) => plugin.defaults)
    }

    static getWidget(widgetType: WidgetType) {
        if (!this.widgetTypes[widgetType]) return undefined
        return this.widgetMap.get(widgetType)
    }

    static newWidget(widgetType: WidgetType) {
        if (!this.widgetMap.has(widgetType)) return undefined
        const widget = this.widgetMap.get(widgetType) as WidgetPlugin
        const id = generateId()

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

function withDefaultWidgets(EditorApp: React.FC) {
    WIDGETS.forEach((w: WidgetPlugin<any>) => {
        if (!w.getType()) return
        if (WidgetFactory.hasWidget(w.getType())) return
        WidgetFactory.register(w.getType(), w)
    })

    return (props: any) => {
        return <EditorApp {...props} panelGroup={WidgetGroupType.PANEL} />
    }
}

function withReportWidgets(EditorApp: React.FC) {
    WIDGETS.forEach((w: WidgetPlugin<any>) => {
        if (!w.getType()) return
        if (WidgetFactory.hasWidget(w.getType())) return
        WidgetFactory.register(w.getType(), w)
    })

    return (props: any) => {
        return <EditorApp {...props} panelGroup={WidgetGroupType.REPORT} />
    }
}

function useWidget(widgetType: string) {
    const [widget, setWidget] = useState<WidgetPlugin | undefined>(WidgetFactory.getWidget(widgetType))

    useEffect(() => {
        const temp = WidgetFactory.getWidget(widgetType)

        // for hot load, factory will be truncated
        if (!temp) {
            WIDGETS.forEach((w: WidgetPlugin<any>) => {
                if (!w.getType()) return
                WidgetFactory.register(w.getType(), w)
            })
        }

        if (temp && temp !== widget) {
            setWidget(temp)
        }

        // @FIXME dynamic Async load the plugin if none exists
        // importPlugin(pluginId)
        //   .then((result) => setPlugin(result))
        //   .catch((err: Error) => {
        //     setError(err.message);
        //   });
    }, [widget, widgetType])

    return {
        widget,
        setWidget,
    }
}

export { withDefaultWidgets, withReportWidgets, useWidget }

export default WidgetFactory

// // @ts-ignore
if (import.meta.hot) {
    // @ts-ignore
    import.meta.hot.accept(() => {})
}
