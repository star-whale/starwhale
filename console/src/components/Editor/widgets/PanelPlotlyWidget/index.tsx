import React from 'react'
import { WidgetRendererProps, WidgetConfig } from '../../Widget/const'
import WidgetPlugin from '../../Widget/WidgetPlugin'
import PanelTable from './component/Table'

export const CONFIG: WidgetConfig = {
    type: 'ui:panel:table',
    group: 'panel',
    name: 'table',
}

function PanelTableWidget(props: WidgetRendererProps) {
    const { data } = props
    const { columnTypes, records } = data

    const columns = React.useMemo(() => {
        return columnTypes?.map((column) => column.name)?.sort((a) => (a === 'id' ? -1 : 1)) ?? []
    }, [columnTypes])

    const panelData = React.useMemo(() => {
        if (!records) return []

        return (
            records?.map((item) => {
                return columns.map((k) => item?.[k])
            }) ?? []
        )
    }, [records, columns])

    return <PanelTable columns={columns} data={panelData} />
}

const widget = new WidgetPlugin(PanelTableWidget, CONFIG)

export default widget
