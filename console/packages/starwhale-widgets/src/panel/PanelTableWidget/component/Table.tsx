import Table from '@/components/Table'
import React from 'react'

export default function PanelTable({ columns, data }) {
    return (
        <Table
            overrides={{
                Root: {
                    style: {
                        width: '100%',
                        height: '100%',
                    },
                },
            }}
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
