import React from 'react'
import { ColumnHintsDesc, ColumnSchemaDesc, RecordSchemaT, isSearchColumns } from '@starwhale/core/datastore'
import { ColumnT } from '../../base/data-table/types'
import DataViewer from '@starwhale/ui/Viewer/DataViewer'
import { RecordAttr } from '../recordAttrModel'
import CustomColumn from '@starwhale/ui/base/data-table/column-custom'
import { FilterBuilderByColumnType, SearchFieldSchemaT } from '@starwhale/ui/Search'

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
        columnTypes?: ColumnSchemaDesc[]
        columnHints?: Record<string, ColumnHintsDesc>
    } = {
        fillWidth: false,
    }
): ColumnT[] {
    const { columnTypes, columnHints } = options

    const searchColumns = React.useMemo(() => {
        if (!columnTypes) return []
        const arr: SearchFieldSchemaT[] = []
        const columns = columnTypes.filter((column) => isSearchColumns(column.name as string))
        columns.forEach(({ name }) => {
            if (!name) return
            arr.push({
                id: name,
                type: name,
                label: name,
                name,
            })
        })

        return arr.sort(sortColumn)
    }, [columnTypes])

    const columns = React.useMemo(() => {
        const columnsWithAttrs: ColumnT[] = []

        columnTypes
            ?.filter((column) => !!column?.name)
            .filter((column) => {
                return isSearchColumns(column?.name as string)
            })
            .sort(sortColumn)
            .forEach((column) => {
                if (!column?.type) return
                if (!column?.name) return
                const { name, type } = column

                const { columnValueHints } = columnHints?.[name] || {}
                const fieldOptions = searchColumns
                const valueOptions = columnValueHints?.map((v) => {
                    return {
                        id: v,
                        type: v,
                        label: v,
                    }
                })
                const getFilters = () =>
                    FilterBuilderByColumnType(type, {
                        fieldOptions,
                        valueOptions,
                    })

                const build =
                    (cached) =>
                    (_FilterBuilder, _options = {}) =>
                        _FilterBuilder({ ...cached, ..._options })

                columnsWithAttrs.push(
                    CustomColumn<RecordAttr, any>({
                        // @ts-ignore
                        columnType: column,
                        key: name,
                        title: name,
                        fillWidth: options.fillWidth,
                        renderCell: RenderMixedCell as any,
                        mapDataToValue: (record: Record<string, RecordSchemaT>): RecordAttr => {
                            return RecordAttr.decode(record, name)
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
