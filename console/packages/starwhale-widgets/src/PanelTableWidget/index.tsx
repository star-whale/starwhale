import React from 'react'
import { WidgetRendererProps, WidgetConfig, WidgetGroupType } from '@starwhale/core/types'
import { WidgetPlugin } from '@starwhale/core/widget'
import PanelTable from './component/Table'
import { ITableState } from '@starwhale/ui/base/data-table/store'
import { useDatastoreMixedSchema } from '@starwhale/core/datastore'

export const CONFIG: WidgetConfig = {
    type: 'ui:panel:table',
    group: WidgetGroupType.PANEL,
    name: 'Table',
    optionConfig: {},
    fieldConfig: {
        uiSchema: {},
        schema: {},
    },
}

function PanelTableWidget(props: WidgetRendererProps<any, any>) {
    const { optionConfig, data = {}, id, onOptionChange } = props
    const storeRef = React.useRef<ITableState | null>(null)

    React.useEffect(() => {
        if (storeRef.current?.isInit || !storeRef.current) return
        storeRef.current.initStore(optionConfig as ITableState)
    }, [optionConfig, storeRef])

    const onChange = React.useCallback(
        (newState: ITableState) => {
            onOptionChange?.(newState.getRawConfigs())
        },
        [onOptionChange]
    )

    const { records } = useDatastoreMixedSchema(data.records)

    return (
        <PanelTable
            columnTypes={data.columnTypes}
            data={records}
            storeKey={id}
            onChange={onChange}
            storeRef={storeRef}
        />
    )
}

const widget = new WidgetPlugin(PanelTableWidget, CONFIG)

export default widget
