import React from 'react'
import { RecordSchemaT, isSearchColumns } from '@starwhale/core/datastore'
import { CustomColumn } from '../../base/data-table'
import { ColumnT } from '../../base/data-table/types'
import DataViewer from '@starwhale/ui/Viewer/DataViewer'
import { RecordAttr } from '../recordAttrModel'

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
    columnTypes?: { name: string; type: string }[],
    options: {
        fillWidth?: boolean
        parseLink?: (link: string) => (str: any) => string
        showPrivate?: boolean
        showLink?: boolean
    } = {
        fillWidth: false,
    }
): ColumnT[] {
    const columns = React.useMemo(() => {
        const columnsWithAttrs: ColumnT[] = []

        columnTypes
            ?.filter((column) => !!column)
            .filter((column) => {
                return isSearchColumns(column.name)
            })
            .sort(sortColumn)
            .forEach((column) => {
                columnsWithAttrs.push(
                    CustomColumn<RecordAttr, any>({
                        columnType: column,
                        key: column.name,
                        title: column.name,
                        fillWidth: options.fillWidth,
                        renderCell: RenderMixedCell as any,
                        mapDataToValue: (record: Record<string, RecordSchemaT>): RecordAttr => {
                            return RecordAttr.decode(record, column.name, options)
                        },
                    })
                )
            })

        return columnsWithAttrs
        // eslint-disable-next-line react-hooks/exhaustive-deps
    }, [columnTypes, options.fillWidth, options.showLink, options.fillWidth, options.parseLink, options.showPrivate])

    return columns
}
