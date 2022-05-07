import React, { useCallback, useRef } from 'react'
import Table, { AutoResizer } from '@/components/BaseTable'
import _ from 'lodash'
import { useTableGridState, ITableGridState } from './useTableGridState'

export interface ITableGridProps extends Partial<ITableGridState> {
    data: any[]
    columns: any[]
    dataConfigMap?: Record<string, any>
    tableConfigMap?: Record<string, any>
}

export default function TableGrid({
    data: rawData = [],
    columns: rawColumns = [],
    dataConfigMap: rawDataConfigMap = {},
    tableConfigMap: rawTabelConfigMap = {},
}: ITableGridProps) {
    const { gridState, handleColumnSort, columns, tableConfigMap } = useTableGridState({
        data: rawData,
        columns: rawColumns,
        dataConfigMap: rawDataConfigMap,
        tableConfigMap: rawTabelConfigMap,
    })

    const getRowClassName = useCallback(
        ({ rowData }: any) => {
            const selected = _.get(gridState.dataConfigMap, [rowData?.id, 'selected'])
            return selected ? 'selected' : ''
        },
        [gridState.dataConfigMap]
    )

    const tableRef = useRef(null)

    return (
        <div
            style={{
                transform: 'translate3d(0px, 0px, 0px)',
                width: '100%',
                boxSizing: 'border-box',
                height: '100%',
            }}
        >
            <AutoResizer>
                {({ width, height }) => (
                    <Table
                        ref={tableRef}
                        width={width}
                        height={height}
                        fixed
                        columns={columns}
                        columnCount={columns}
                        rowClassName={getRowClassName}
                        data={gridState.data}
                        sortBy={
                            tableConfigMap?.sortBy
                                ? {
                                      key: tableConfigMap?.sortBy?.key as React.Key,
                                      order: tableConfigMap?.sortBy?.order,
                                  }
                                : undefined
                        }
                        onColumnSort={({ key, order }) => handleColumnSort({ sortBy: { key: key as string, order } })}
                    />
                )}
            </AutoResizer>
        </div>
    )
}
