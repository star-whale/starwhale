// @ts-nocheck
/* eslint-disable react/prop-types */
import React from 'react'
import Table, { Column, SortOrder, AutoResizer } from '@/components/BaseTable'
import { makeData } from '@/components/Table/TableGridMock'

const { columns, data, sortBy } = makeData(100)
export default class JobTest extends React.Component {
    state = {
        columns: columns,
        data: data,
        sortBy: sortBy,
    }

    onColumnSort = (sortBy) => {
        const order = sortBy.order === SortOrder.ASC ? 1 : -1
        const data = [...this.state.data]
        data.sort((a, b) => (a[sortBy.key] > b[sortBy.key] ? order : -order))
        this.setState({
            sortBy,
            data,
        })
    }

    render() {
        const { data, sortBy, columns } = this.state

        return (
            <div
                style={{
                    transform: 'translate3d(0px, 0px, 0px)',
                    width: '100%',
                    boxSizing: 'border-box',
                    height: '100%',
                    padding: '50px 0',
                }}
            >
                <AutoResizer>
                    {({ width, height }) => (
                        <Table
                            width={width}
                            height={height}
                            fixed
                            columns={columns}
                            columnCount={columns.length}
                            data={data}
                            sortBy={sortBy}
                            onColumnSort={this.onColumnSort}
                        />
                    )}
                </AutoResizer>
            </div>
        )
    }
}
