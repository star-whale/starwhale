import React from 'react'
import { useStoreApi, useStore } from './useStore'
import { ConfigQuery, ConfigQueryInline } from '../components/Query'
import { ColumnT } from '@starwhale/ui/base/data-table/types'
import { ITableState } from '../store'

const selector = (state: ITableState) => ({
    queries: state.currentView?.queries ?? [],
    onCurrentViewQueriesChange: state.onCurrentViewQueriesChange ?? [],
    columnTypes: state.columnTypes,
})

function useGridQuery({ columns }: { columns: ColumnT[] }) {
    const store = useStoreApi()
    const { queries, onCurrentViewQueriesChange, columnTypes } = useStore(selector)

    const onChange = React.useCallback((items) => onCurrentViewQueriesChange(items), [onCurrentViewQueriesChange])

    const renderConfigQuery = React.useCallback(() => {
        return <ConfigQuery columns={columns} value={queries} onChange={onChange} columnTypes={columnTypes} />
    }, [columnTypes, queries, onChange])

    const renderConfigQueryInline = React.useCallback(
        ({ width }: { width: number }) => {
            return (
                <ConfigQueryInline
                    columns={columns}
                    value={queries}
                    onChange={onChange}
                    width={width}
                    columnTypes={columnTypes}
                />
            )
        },
        [columnTypes, queries, onChange]
    )

    return {
        renderConfigQuery,
        renderConfigQueryInline,
        value: queries,
        columns,
        onChange,
    }
}

export { useGridQuery }
export default useGridQuery
