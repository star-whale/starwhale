import React from 'react'
import { WidgetRendererProps, WidgetConfig, WidgetGroupType } from '@starwhale/core/types'
import { WidgetPlugin } from '@starwhale/core/widget'
import PanelTable from './component/Table'

export const CONFIG: WidgetConfig = {
    type: 'ui:panel:table',
    group: WidgetGroupType.PANEL,
    name: 'table',
}

function PanelTableWidget(props: WidgetRendererProps) {
    const { data } = props
    const { columnTypes, records } = data

    const columns = React.useMemo(() => {
        return columnTypes?.map((column: any) => column.name)?.sort((a) => (a === 'id' ? -1 : 1)) ?? []
    }, [columnTypes])

    const panelData = React.useMemo(() => {
        if (!records) return []

        return (
            records?.map((item: any) => {
                return columns.map((k: any) => item?.[k])
            }) ?? []
        )
    }, [records, columns])

    return <PanelTable columns={columns} data={panelData} />
}

const widget = new WidgetPlugin(PanelTableWidget, CONFIG)

export default widget
