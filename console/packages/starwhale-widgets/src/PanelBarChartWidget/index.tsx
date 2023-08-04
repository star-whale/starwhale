import BusyPlaceholder from '@starwhale/ui/BusyLoaderWrapper/BusyPlaceholder'
import React from 'react'
import { WidgetConfig, WidgetGroupType, WidgetRendererProps } from '@starwhale/core/types'
import { WidgetPlugin } from '@starwhale/core/widget'
import { UI_DATA } from '@starwhale/core/form/schemas/fields'
import { getBarChartConfig } from '@starwhale/ui/Plotly/utils'
import { decordRecords, getTableShortName } from '@starwhale/core/datastore'

const PlotlyViewer = React.lazy(() => import(/* webpackChunkName: "PlotlyViewer" */ '@starwhale/ui/Plotly'))

export const CONFIG: WidgetConfig = {
    type: 'ui:panel:barchart',
    group: WidgetGroupType.PANEL,
    name: 'Bar Chart',
    fieldConfig: {
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
            x: {
                title: 'X',
                type: 'string',
            },
            y: {
                title: 'Y',
                type: 'string',
            },
        },
        uiSchema: {
            x: {
                /**
                 * framework define field uiSchema
                 * 1. UI_DATA.DataTableColumns will auto fill this field by columnTypes
                 */
                'ui:data': UI_DATA.DataTableColumns,
            },
            y: {
                'ui:data': UI_DATA.DataTableColumns,
            },
        },
    },
}

function PanelBarChartWidget(props: WidgetRendererProps<any, any>) {
    // console.log('PanelRocAucWidget', props)

    const { fieldConfig, data = {} } = props
    const { getTableRecordMap } = data
    const { data: formData } = fieldConfig ?? {}
    const { chartTitle: title, x: xattr, y: yattr } = formData ?? {}

    const m = getTableRecordMap()

    const barData = Object.entries(m).map(([k, records]) => {
        const x: number[] = []
        const y: number[] = []
        if (records)
            decordRecords(records as any).forEach((item: any) => {
                const xnum = item?.[xattr]
                const ynum = item?.[yattr]
                if (xnum && !Number.isNaN(xnum)) x.push(Number(xnum))
                if (ynum && !Number.isNaN(ynum)) y.push(Number(ynum))
            })
        return {
            x,
            y,
            type: 'bar',
            name: getTableShortName(k),
        }
    })

    const vizData = getBarChartConfig(title, { x: xattr, y: yattr }, barData as any)

    return (
        <React.Suspense fallback={<BusyPlaceholder />}>
            <PlotlyViewer data={vizData} />
        </React.Suspense>
    )
}

const widget = new WidgetPlugin(PanelBarChartWidget, CONFIG)

export default widget
