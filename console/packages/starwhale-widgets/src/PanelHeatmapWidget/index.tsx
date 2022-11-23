import BusyPlaceholder from '@/components/BusyLoaderWrapper/BusyPlaceholder'
import { getHeatmapConfig } from '@/components/Indicator/utils'
import { useParseConfusionMatrix } from '@/domain/datastore/hooks/useParseDatastore'
import React from 'react'
import { WidgetConfig, WidgetGroupType, WidgetRendererProps } from '@starwhale/core/widget'
import { WidgetPlugin } from '@starwhale/core/widget'

const PlotlyVisualizer = React.lazy(
    () => import(/* webpackChunkName: "PlotlyVisualizer" */ '@/components/Indicator/PlotlyVisualizer')
)

export const CONFIG: WidgetConfig = {
    type: 'ui:panel:confusion_matrix',
    group: WidgetGroupType.PANEL,
    name: 'Confusion Matrix',
}

function PanelHeatmapWidget(props: WidgetRendererProps<any, any>) {
    const { fieldConfig, data = {} } = props
    const { data: formData } = fieldConfig ?? {}
    const title = formData?.chartTitle ?? ''

    const { labels, binarylabel } = useParseConfusionMatrix(data)
    const heatmapData = getHeatmapConfig(title, labels, binarylabel)

    return (
        <React.Suspense fallback={<BusyPlaceholder />}>
            <PlotlyVisualizer data={heatmapData} />
        </React.Suspense>
    )
}

const widget = new WidgetPlugin(PanelHeatmapWidget, CONFIG)

export default widget
