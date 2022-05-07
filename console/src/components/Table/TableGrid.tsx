import React, { useCallback, useRef, forwardRef } from 'react'
import Table, { Column, SortOrder, AutoResizer } from '@/components/BaseTable'
import _ from 'lodash'
import { useTableGridState, ITableGridState } from './useTableGridState'

export interface ITableGridProps extends Partial<ITableGridState> {
    data: any[]
    columns: any[]
    dataConfigMap?: {}
    tableConfigMap?: {}
}

export default function TableGrid(props: ITableGridProps) {
    const { gridState, handleColumnSort, handleRowSelect, columns, data, tableConfigMap } = useTableGridState({
        data: props.data || [],
        columns: props.columns || [],
        dataConfigMap: props.dataConfigMap || {},
        tableConfigMap: props.tableConfigMap || {},
    })

    const getRowClassName = useCallback(
        ({ rowData, rowIndex }: any) => {
            const selected = _.get(gridState.dataConfigMap, [rowData?.id, 'selected'])
            return selected ? 'selected' : ''
        },
        [gridState.dataConfigMap]
    )

    const tableRef = useRef(null)

    console.log(gridState, tableConfigMap?.sortBy)

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
                        onColumnSort={({ column, key, order }) =>
                            handleColumnSort({ sortBy: { key: key as string, order } })
                        }
                    />
                )}
            </AutoResizer>
        </div>
    )
}
