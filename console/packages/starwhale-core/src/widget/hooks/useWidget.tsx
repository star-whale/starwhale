import { useEffect, useState } from 'react'
import DNDListWidget from '@starwhale/widgets/DNDListWidget'
import SectionWidget from '@starwhale/widgets/SectionWidget'
import PanelTableWidget from '@starwhale/widgets/PanelTableWidget'
import PanelRocAucWidget from '@starwhale/widgets/PanelRocAucWidget'
import PanelHeatmapWidget from '@starwhale/widgets/PanelConfusionMatrixWidget'
import WidgetFactory from '../WidgetFactory'
import WidgetPlugin from '../WidgetPlugin'

const modules = [DNDListWidget, SectionWidget, PanelTableWidget, PanelRocAucWidget, PanelHeatmapWidget]

export function useWidget(widgetType: string) {
    const [widget, setWidget] = useState<WidgetPlugin | undefined>(WidgetFactory.widgetMap.get(widgetType))

    useEffect(() => {
        if (!WidgetFactory.widgetMap.has(widgetType)) {
            modules.forEach((w: WidgetPlugin<any>) => {
                if (w.getType() === widgetType) WidgetFactory.register(w.getType(), w)
            })
        }

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
