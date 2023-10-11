import React from 'react'
import { WidgetRendererProps, WidgetConfig, WidgetGroupType } from '@starwhale/core/types'
import { WidgetPlugin } from '@starwhale/core/widget'
import { GridTable } from '@starwhale/ui/GridTable'
import { ITableState } from '@starwhale/ui/GridTable/store'
import _ from 'lodash'
import { useLocalStorage } from 'react-use'

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

function toArray(value: any) {
    if (_.isArray(value)) return value
    if (value) return [value]
    return undefined
}

function PanelTableWidget(props: WidgetRendererProps<any, any>) {
    const { fieldConfig, optionConfig, data = {}, id, onOptionChange, page, onPageChange, readonly } = props

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

    const tables = toArray(fieldConfig?.data?.tableName)

    const num = tables?.length

    const [debug] = useLocalStorage('debug', false)

    return (
        <>
            <GridTable
                columnTypes={data.columnTypes}
                columnHints={data.columnHints}
                records={data.records}
                storeKey={id}
                queryinline={num === 1 && !readonly}
                sortable={num === 1}
                columnleinline={!readonly}
                previewable
                fillable
                paginationable
                currentView={optionConfig?.currentView}
                page={page}
                onPageChange={onPageChange}
                onCurrentViewChange={onCurrentViewChange}
                onInit={onInit}
            />
            {Boolean(debug) && tables?.join(',')}
        </>
    )
}

const widget = new WidgetPlugin(PanelTableWidget, CONFIG)

export default widget
