import { useEffect, useState } from 'react'
import DNDListWidget from '@starwhale/widgets/DNDListWidget'
import SectionWidget from '@starwhale/widgets/SectionWidget'
import PanelTableWidget from '@starwhale/widgets/PanelTableWidget'
import PanelRocAucWidget from '@starwhale/widgets/PanelRocAucWidget'
import PanelHeatmapWidget from '@starwhale/widgets/PanelConfusionMatrixWidget'
import { WidgetConfig } from '../types'
import WidgetFactory from './WidgetFactory'
import WidgetPlugin from './WidgetPlugin'

export function useWidget(widgetType: string) {
    const [widget, setWidget] = useState<WidgetPlugin | undefined>(WidgetFactory.widgetMap.get(widgetType))

    useEffect(() => {
        const temp = WidgetFactory.widgetMap.get(widgetType)

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

export const registerWidget = (Widget: any, config: WidgetConfig) => {
    if (!config?.type) {
        // eslint-disable-next-line no-console
        console.log('Widget registration missing type', config)
        return
    }

    WidgetFactory.register(config.type, Widget)
}

const modules = [DNDListWidget, SectionWidget, PanelTableWidget, PanelRocAucWidget, PanelHeatmapWidget]
export const registerWidgets = () => {
    const start = performance.now()
    modules.forEach((widget) => {
        if (widget.defaults.type in WidgetFactory.widgetTypes) return
        registerWidget(widget, widget.defaults)
    })
    // eslint-disable-next-line no-console
    console.log('Widget registration took: ', performance.now() - start, 'ms')
}
