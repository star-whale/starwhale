import React from 'react'
import { WidgetRendererProps, WidgetConfig, WidgetGroupType } from '@starwhale/core/types'
import { WidgetPlugin } from '@starwhale/core/widget'
import { GridTable } from '@starwhale/ui/GridTable'
import { ITableState } from '@starwhale/ui/GridTable/store'

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

    const onCurrentViewChange = React.useCallback(
        (newState: ITableState) => {
            onOptionChange?.(newState.getRawConfigs())
        },
        [onOptionChange]
    )

    const onInit = React.useCallback(
        ({ initStore }) => {
            initStore?.(optionConfig)
        },
        [optionConfig]
    )

    return (
        <GridTable
            columnTypes={data.columnTypes}
            records={data.records}
            storeKey={id}
            queryinline
            previewable
            fillable
            onCurrentViewChange={onCurrentViewChange}
            onInit={onInit}
        />
    )
}

const widget = new WidgetPlugin(PanelTableWidget, CONFIG)

export default widget
