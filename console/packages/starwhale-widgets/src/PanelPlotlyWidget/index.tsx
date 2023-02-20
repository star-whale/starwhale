import BusyPlaceholder from '@starwhale/ui/BusyLoaderWrapper/BusyPlaceholder'
import React from 'react'
import { WidgetConfig, WidgetGroupType, WidgetRendererProps } from '@starwhale/core/types'
import { WidgetPlugin } from '@starwhale/core/widget'

const PlotlyViewer = React.lazy(() => import(/* webpackChunkName: "PlotlyViewer" */ '@starwhale/ui/Plotly'))

export const CONFIG: WidgetConfig = {
    type: 'ui:panel:ploty',
    group: WidgetGroupType.PANEL,
    name: 'Ploty',
}

function PanelPlotyWidget(props: WidgetRendererProps<any, any>) {
    const { data = {} } = props

    return (
        <React.Suspense fallback={<BusyPlaceholder />}>
            <PlotlyViewer data={data} />
        </React.Suspense>
    )
}

const widget = new WidgetPlugin(PanelPlotyWidget, CONFIG)

export default widget
