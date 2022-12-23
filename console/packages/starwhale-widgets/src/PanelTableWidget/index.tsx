import React from 'react'
import { WidgetRendererProps, WidgetConfig, WidgetGroupType } from '@starwhale/core/types'
import { WidgetPlugin } from '@starwhale/core/widget'
import PanelTable from './component/Table'

export const CONFIG: WidgetConfig = {
    type: 'ui:panel:table',
    group: WidgetGroupType.PANEL,
    name: 'Table',
    fieldConfig: {
        uiSchema: {},
        schema: {},
    },
}

function PanelTableWidget(props: WidgetRendererProps<any, any>) {
    const { data = {}, id } = props
    const { columnTypes = [], records = [] } = data

    return <PanelTable columnTypes={columnTypes} data={records} storeKey={id} />
}

const widget = new WidgetPlugin(PanelTableWidget, CONFIG)

export default widget
