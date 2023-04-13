/* eslint-disable */

import * as React from 'react'

import { useStyletron } from 'baseui'

import Column from './column'
import { COLUMNS } from './constants'
import { HighlightCellText } from './text-search'
import { ColumnT, FilterTypes, SharedColumnOptionsT } from './types'

type OptionsT = {
    lineClamp?: number
} & SharedColumnOptionsT<string>

type FilterParametersT = {
    description: string
    exclude: boolean
}

type StringColumnT = ColumnT<string, FilterParametersT>

function StringFilter(props: any) {
    return <div>not implemented for string column</div>
}

export function StringCell(props: any) {
    return (
        <div title={props.value} className='string-cell'>
            {props.textQuery ? <HighlightCellText text={props.value} query={props.textQuery} /> : props.value}
        </div>
    )
}

function StringColumn(options: OptionsT): StringColumnT {
    return Column({
        kind: COLUMNS.STRING,
        cellBlockAlign: options.cellBlockAlign,
        buildFilter: function (params) {
            return function (data) {
                return true
            }
        },
        fillWidth: options.fillWidth,
        filterable: false,
        mapDataToValue: options.mapDataToValue,
        maxWidth: options.maxWidth,
        minWidth: options.minWidth,
        // @ts-ignore
        renderCell: function RenderStringCell(props: any) {
            return <StringCell {...props} lineClamp={options.lineClamp} />
        },
        // @ts-ignore
        renderFilter: StringFilter,
        sortable: options.sortable === undefined ? true : options.sortable,
        sortFn: function (a, b) {
            return a.localeCompare(b)
        },
        textQueryFilter: function (textQuery = '', data = '') {
            if (!data) return false

            return data.toLowerCase().includes(textQuery.toLowerCase())
        },
        title: options.title,
        key: options.key,
        pin: options.pin,
        filterType: options.filterType ?? FilterTypes.string,
        columnType: options.columnType,
    })
}

export default StringColumn
