import Table from '@/components/Table'
import React from 'react'

export default function PanelTable({ columns, data }) {
    return (
        <Table
            columns={columns}
            data={data}
            // paginationProps={{
            //     start: modelsInfo.data?.pageNum,
            //     count: modelsInfo.data?.pageSize,
            //     total: modelsInfo.data?.total,
            //     afterPageChange: () => {
            //         info.refetch()
            //     },
            // }}
        />
    )
}
