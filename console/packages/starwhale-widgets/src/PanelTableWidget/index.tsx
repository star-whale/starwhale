import React from 'react'
import { WidgetRendererProps, WidgetConfig, WidgetGroupType } from '@starwhale/core/types'
import { WidgetPlugin } from '@starwhale/core/widget'
import PanelTable from './component/Table'
import { ITableState } from '@starwhale/ui/base/data-table/store'

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
    const { columnTypes = [], records = [] } = data
    const [state, setState] = React.useState<ITableState | null>(null)
    const storeRef = React.useRef<ITableState | null>(null)

    React.useEffect(() => {
        if (state?.isInit || !storeRef.current) return
        // console.log('--paneltable--', storeRef.current, optionConfig)
        storeRef.current.initStore(optionConfig as ITableState)
        setState(optionConfig as ITableState)
    }, [optionConfig, storeRef, state])

    const onChange = React.useCallback(
        (newState: ITableState) => {
            onOptionChange?.(newState.getRawConfigs())
        },
        [onOptionChange]
    )

    return <PanelTable columnTypes={columnTypes} data={records} storeKey={id} onChange={onChange} storeRef={storeRef} />
}

const widget = new WidgetPlugin(PanelTableWidget, CONFIG)

export default widget
