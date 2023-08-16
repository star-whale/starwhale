import BusyPlaceholder from '@starwhale/ui/BusyLoaderWrapper/BusyPlaceholder'
import React from 'react'
import { WidgetConfig, WidgetGroupType, WidgetRendererProps } from '@starwhale/core/types'
import { WidgetPlugin } from '@starwhale/core/widget'
import { UI_DATA } from '@starwhale/core/form/schemas/fields'
import { getBarChartConfig } from '@starwhale/ui/Plotly/utils'
import { decordRecords } from '@starwhale/core/datastore'

const PlotlyViewer = React.lazy(() => import(/* webpackChunkName: "PlotlyViewer" */ '@starwhale/ui/Plotly'))

export const CONFIG: WidgetConfig = {
    type: 'ui:panel:barchart',
    group: WidgetGroupType.PANEL,
    name: 'Bar Chart',
    fieldConfig: {
        data: {
            labels: ['sys/model_name'],
        },
        schema: {
            /**
             * framework define field
             * 1. tableName used to select table names
             */
            tableName: {
                /**
                 * framework define field type
                 * 1. use type = 'array' to make field multiple
                 */
                type: 'array',
            },
            labels: {
                title: 'Axis Label',
                type: 'array',
            },
            metrics: {
                title: 'Metrics',
                type: 'array',
            },
        },
        uiSchema: {
            labels: {
                /**
                 * framework define field uiSchema
                 * 1. UI_DATA.DataTableColumns will auto fill this field by columnTypes
                 */
                'ui:data': UI_DATA.DataTableColumns,
            },
            metrics: {
                'ui:data': UI_DATA.DataTableColumns,
            },
        },
    },
}

function PanelBarChartWidget(props: WidgetRendererProps<any, any>) {
    const { fieldConfig, data = {} } = props
    const { getTableRecordMap } = data
    const { data: formData } = fieldConfig ?? {}
    const { chartTitle: title, labels: xattr = [], metrics: yattr = [] } = formData ?? {}
    const m = getTableRecordMap()
    const barData: { x: any[]; y: any[]; type: string; name: string }[] = []

    Object.entries(m).forEach(([, records]) => {
        if (records)
            decordRecords(records as any).forEach((item: any) => {
                const x: any[] = []
                const y: any[] = []
                const names: string[] = []

                Array.from(yattr).forEach((_y: string) => {
                    x.push(_y)
                    y.push(item?.[_y])
                })

                Array.from(xattr).forEach((_x: string) => {
                    names.push(item?.[_x])
                })

                //  getTableShortName(k)]
                barData.push({
                    x,
                    y,
                    type: 'bar',
                    name: names.join(' '),
                })
            })
    })

    const vizData = getBarChartConfig(title, undefined, barData as any)

    return (
        <React.Suspense fallback={<BusyPlaceholder />}>
            <PlotlyViewer data={vizData} />
        </React.Suspense>
    )
}

const widget = new WidgetPlugin(PanelBarChartWidget, CONFIG)

export default widget
