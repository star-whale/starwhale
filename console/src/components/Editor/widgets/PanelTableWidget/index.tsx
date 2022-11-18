import React from 'react'
import { WidgetRendererProps, WidgetConfig } from '../../Widget/const'
import WidgetPlugin from '../../Widget/WidgetPlugin'
import PanelTable from './component/Table'

export const CONFIG: WidgetConfig = {
    type: 'ui:panel:table',
    group: 'panel',
    name: 'table',
    fieldConfig: {
        uiSchema: {
            'ui:order': ['*', 'chartTitle'],
        },
        schema: {
            type: 'object',
            properties: {
                tableName: {
                    'ui:widget': 'DatastoreTableSelect',
                },
            },
        },
        data: {
            chartType: 'ui:panel:table',
        },
        // dataOverrides: {
        //     tableName: '',
        //     chartTitle: 'summary',
        // },
    },
}

function PanelTableWidget(props: WidgetRendererProps<any, any>) {
    // console.log('PanelTableWidget', props)

    const { data = {} } = props
    const { columnTypes = [], records = [] } = data

    const columns = React.useMemo(() => {
        return columnTypes.map((column: any) => column.name)?.sort((a) => (a === 'id' ? -1 : 1)) ?? []
    }, [columnTypes])

    const $data = React.useMemo(() => {
        if (!records) return []

        return (
            records.map((item: any) => {
                return columns.map((k) => item?.[k])
            }) ?? []
        )
    }, [records, columns])

    return (
        <div style={{ width: '100%', height: '100%' }}>
            <PanelTable columns={columns} data={$data} />
        </div>
    )
}

const widget = new WidgetPlugin(PanelTableWidget, CONFIG)

export default widget
