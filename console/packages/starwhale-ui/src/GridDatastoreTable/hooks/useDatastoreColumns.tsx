import React from 'react'
import { RecordSchemaT, isComplexType, isSearchColumns } from '@starwhale/core/datastore'
import { CustomColumn } from '../../base/data-table'
import { ColumnT, RenderCellT } from '../../base/data-table/types'
import { StringCell } from '../../base/data-table/column-string'
import _ from 'lodash'
import { RecordT, getSummary } from '@starwhale/core/dataset'
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

    return ca.name.localeCompare(cb.name)
}

function RenderMixedCell({ value, data, columnKey, ...props }: RenderCellT<any>['props'] & { options: any }) {
    if (isComplexType(value.type)) return <DataViewer data={value} showKey={columnKey} />
    return <StringCell {...props} lineClamp={1} value={value.toString()} />
}

export function useDatastoreColumns(columnTypes?: { name: string; type: string }[], options = {}): ColumnT[] {
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
                        fillWidth: false,
                        renderCell: (props: any) => <RenderMixedCell {...props} />,
                        mapDataToValue: (record: Record<string, RecordSchemaT>): RecordAttr => {
                            return RecordAttr.decode(record, column.name, options)
                        },
                    })
                )
            })

        return columnsWithAttrs
    }, [columnTypes])

    return columns
}
