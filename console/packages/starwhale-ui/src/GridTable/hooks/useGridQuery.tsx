import React from 'react'
import { useStore } from './useStore'
import { ConfigQuery, ConfigQueryInline } from '../components/Query'
import { ColumnT } from '@starwhale/ui/base/data-table/types'
import shallow from 'zustand/shallow'
import { IGridState } from '../types'

const selector = (state: IGridState) => ({
    queries: state.currentView?.queries ?? [],
    onCurrentViewQueriesChange: state.onCurrentViewQueriesChange ?? [],
    columnTypes: state.columnTypes,
})

function useGridQuery({ columns }: { columns: ColumnT[] }) {
    const { queries, onCurrentViewQueriesChange, columnTypes } = useStore(selector, shallow)

    const onChange = React.useCallback((items) => onCurrentViewQueriesChange(items), [onCurrentViewQueriesChange])

    const renderConfigQuery = React.useCallback(() => {
        return <ConfigQuery value={queries} onChange={onChange} columnTypes={columnTypes} />
    }, [columnTypes, queries, onChange])

    const renderConfigQueryInline = React.useCallback(
        ({ width }: { width: number }) => {
            return <ConfigQueryInline value={queries} onChange={onChange} width={width} columnTypes={columnTypes} />
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
