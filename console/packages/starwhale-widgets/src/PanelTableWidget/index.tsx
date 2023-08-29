import React from 'react'
import { WidgetRendererProps, WidgetConfig, WidgetGroupType } from '@starwhale/core/types'
import { WidgetPlugin } from '@starwhale/core/widget'
import { GridTable } from '@starwhale/ui/GridTable'
import { ITableState } from '@starwhale/ui/GridTable/store'
import _ from 'lodash'

export const CONFIG: WidgetConfig = {
    type: 'ui:panel:table',
    group: [WidgetGroupType.ALL, WidgetGroupType.PANEL],
    name: 'Table',
    optionConfig: {},
    fieldConfig: {
        uiSchema: {},
        schema: {
            tableName: {
                type: 'array',
            },
        },
    },
}

function PanelTableWidget(props: WidgetRendererProps<any, any>) {
    const { fieldConfig, optionConfig, data = {}, id, onOptionChange, page, onPageChange } = props

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

    const tables = _.isArray(fieldConfig?.data?.tableName)
        ? fieldConfig?.data?.tableName?.length
        : Number(Boolean(fieldConfig?.data?.tableName))

    return (
        <GridTable
            columnTypes={data.columnTypes}
            records={data.records}
            storeKey={id}
            queryinline={tables === 1}
            sortable={tables === 1}
            columnleinline
            previewable
            fillable
            paginationable
            currentView={optionConfig?.currentView}
            page={page}
            onPageChange={onPageChange}
            onCurrentViewChange={onCurrentViewChange}
            onInit={onInit}
        />
    )
}

const widget = new WidgetPlugin(PanelTableWidget, CONFIG)

export default widget
