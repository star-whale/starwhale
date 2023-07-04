import { useEffect, useState } from 'react'
import WidgetFactory from '../WidgetFactory'
import WidgetPlugin from '../WidgetPlugin'

export function useWidget(widgetType: string) {
    const [widget, setWidget] = useState<WidgetPlugin | undefined>(WidgetFactory.getWidget(widgetType))

    useEffect(() => {
        const temp = WidgetFactory.getWidget(widgetType)

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
