import BusyPlaceholder from '@/components/BusyLoaderWrapper/BusyPlaceholder'
import { getRocAucConfig } from '@/components/Indicator/utils'
import { useParseRocAuc } from '@/domain/datastore/hooks/useParseDatastore'
import React from 'react'
import { WidgetConfig, WidgetRendererProps } from '../../Widget/const'
import WidgetPlugin from '../../Widget/WidgetPlugin'

const PlotlyVisualizer = React.lazy(
    () => import(/* webpackChunkName: "PlotlyVisualizer" */ '@/components/Indicator/PlotlyVisualizer')
)

export const CONFIG: WidgetConfig = {
    type: 'ui:panel:rocauc',
    group: 'panel',
    name: 'Roc Auc',
}

function PanelRocAucWidget(props: WidgetRendererProps<any, any>) {
    console.log('PanelRocAucWidget', props)

    const { fieldConfig, data = {} } = props
    const { records = [] } = data
    const { data: formData } = fieldConfig ?? {}
    const title = formData?.chartTitle ?? ''

    const rocAucData = useParseRocAuc({ records })
    const vizData = getRocAucConfig(title, [], rocAucData as any)

    return (
        <React.Suspense fallback={<BusyPlaceholder />}>
            <PlotlyVisualizer data={vizData} />
        </React.Suspense>
    )
}

const widget = new WidgetPlugin(PanelRocAucWidget, CONFIG)

export default widget
