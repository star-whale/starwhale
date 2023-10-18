import React from 'react'
import {
    ColumnDesc,
    ColumnHintsDesc,
    ColumnSchemaDesc,
    RecordSchemaT,
    isSearchColumns,
} from '@starwhale/core/datastore'
import { ColumnT } from '../../base/data-table/types'
import DataViewer from '@starwhale/ui/Viewer/DataViewer'
import { RecordAttr } from '../recordAttrModel'
import CustomColumn from '@starwhale/ui/base/data-table/column-custom'
import { FilterBuilder, FilterBuilderByColumnType, SearchFieldSchemaT } from '@starwhale/ui/Search'

export const sortColumn = (ca: { name: string }, cb: { name: string }) => {
    if (ca.name === 'sys/id') return -1
    if (cb.name === 'sys/id') return 1
    if (ca.name?.startsWith('sys/') && cb.name?.startsWith('sys/')) {
        return ca.name.localeCompare(cb.name)
    }
    if (ca.name?.startsWith('sys/') && !cb.name?.startsWith('sys/')) {
        return -1
    }
    if (!ca.name?.startsWith('sys/') && cb.name?.startsWith('sys/')) {
        return 1
    }
    if (ca.name === 'id') return -1
    if (cb.name === 'id') return 1

    return ca.name.localeCompare(cb.name)
}

export function RenderMixedCell({ value, columnKey }: { value: RecordAttr; columnKey: string }) {
    if (!value) return ''
    return <DataViewer data={value} showKey={columnKey as string} />
}

export function useDatastoreColumns(
    options: {
        fillWidth?: boolean
        showPrivate?: boolean
        showLink?: boolean
        columnTypes?: { name: string; type: string }[]
        columnHint?: Record<string, ColumnHintsDesc>
    } = {
        fillWidth: false,
    }
): ColumnT[] {
    const { columnTypes, columnHints } = options

    const searchColumns = React.useMemo(() => {
        if (!columnTypes) return []
        const arr: SearchFieldSchemaT[] = []
        const columns = columnTypes.filter((column) => isSearchColumns(column.name))
        columns.forEach((column) => {
            arr.push({
                id: column.name,
                type: column.name,
                label: column.name,
                name: column.name,
            })
        })

        return arr.sort(sortColumn)
    }, [columnTypes])

    const columns = React.useMemo(() => {
        const columnsWithAttrs: ColumnT[] = []

        columnTypes
            ?.filter((column) => !!column)
            .filter((column) => {
                return isSearchColumns(column.name)
            })
            .sort(sortColumn)
            .forEach((column) => {
                const { columnValueHints } = columnHints?.[column.name]
                const fieldOptions = searchColumns
                const valueOptions = columnValueHints?.map((v) => {
                    return {
                        id: v,
                        type: v,
                        label: v,
                    }
                })
                const getFilters = () =>
                    FilterBuilderByColumnType(column.type, {
                        fieldOptions,
                        valueOptions,
                    })

                const build =
                    (cached) =>
                    (_FilterBuilder, _options = {}) =>
                        _FilterBuilder({ ...cached, ..._options })

                columnsWithAttrs.push(
                    CustomColumn<RecordAttr, any>({
                        columnType: column,
                        key: column.name,
                        title: column.name,
                        fillWidth: options.fillWidth,
                        renderCell: RenderMixedCell as any,
                        mapDataToValue: (record: Record<string, RecordSchemaT>): RecordAttr => {
                            return RecordAttr.decode(record, column.name)
                        },
                        // search bar
                        getFilters,
                        buildFilters: build({ fieldOptions, valueOptions }),
                    })
                )
            })

        return columnsWithAttrs
        // eslint-disable-next-line react-hooks/exhaustive-deps
    }, [columnTypes, options.fillWidth, options.showLink, options.fillWidth, options.showPrivate])

    return columns
}
