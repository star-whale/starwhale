import React from 'react'
import { CategoricalColumn, CustomColumn, NumericalColumn, StringColumn } from '../base/data-table'
import CategoricalTagsColumn from '../base/data-table/column-categorical-tags'

export function val(r: any) {
    if (r === undefined) return ''
    if (typeof r === 'object' && 'value' in r) {
        return typeof r.value === 'object' ? JSON.stringify(r.value, null) : r.value
    }

    return r
}

export function normalizeStaticColumns(columns: any) {
    return columns.map((raw: any, index: number) => {
        let column = raw

        if (typeof raw !== 'string') {
            return {
                ...column,
                fillWidth: false,
            }
        }

        // @ts-ignore
        const item = data?.[0]?.[index]
        if (typeof raw === 'string') {
            column = { type: 'string', title: raw, index, sortable: true }
        }
        if (React.isValidElement(item)) {
            column = { type: 'custom', title: raw, index, renderCell: (props: any) => props.value }
        }

        const initColumns = {
            pin: null,
            title: column.title,
            resizable: true,
            cellBlockAlign: 'center',
            index: column.index,
            // @ts-ignore
            sortable: !React.isValidElement(data?.[0]?.[index]),
            sortFn: function sortFn(a: any, b: any) {
                return a.localeCompare(b)
            },
            mapDataToValue: (data: any) => data[index],
            minWidth: 100,
        }

        switch (column.type) {
            case 'string':
                return StringColumn({
                    ...initColumns,
                    ...column,
                })
            case 'number':
                return NumericalColumn({
                    ...initColumns,
                    ...column,
                })
            default:
            case 'custom':
                return CustomColumn({
                    ...initColumns,
                    filterable: true,
                    buildFilter: function buildFilter(params: any) {
                        return function filterHas(data: any) {
                            return params.selection.has(data)
                        }
                    },
                    renderCell: (props: any) => props.value,
                    ...column,
                })
            case 'categorical':
                return CategoricalColumn({
                    ...initColumns,
                    ...column,
                })
            case 'tags':
                return CategoricalTagsColumn({
                    ...initColumns,
                    ...column,
                })
        }
    })
}
