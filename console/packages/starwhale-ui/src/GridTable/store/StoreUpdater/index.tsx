import { useEffect } from 'react'
import { StoreApi } from 'zustand'

import { useStore, useStoreApi } from '../../hooks/useStore'
import type { StatefulDataTablePropsT } from '@starwhale/ui/base/data-table/types'
import { ITableState } from '@starwhale/ui/base/data-table/store'
import shallow from 'zustand/shallow'
import { useDatastoreColumns } from '@starwhale/ui/GridDatastoreTable'
import { val } from '../../utils'

type StoreUpdaterProps = StatefulDataTablePropsT & { rfId: string }

const selector = (s: ITableState) => ({
    // setNodes: s.setNodes,
    // setEdges: s.setEdges,
    // setDefaultNodesAndEdges: s.setDefaultNodesAndEdges,
    // setMinZoom: s.setMinZoom,
    // setMaxZoom: s.setMaxZoom,
    // setTranslateExtent: s.setTranslateExtent,
    // setNodeExtent: s.setNodeExtent,
    // reset: s.reset,
})

export function useStoreUpdater<T>(value: T | undefined, setStoreState: (param: T) => void) {
    useEffect(() => {
        if (typeof value !== 'undefined') {
            setStoreState(value)
        }
    }, [value])
}

// updates with values in store that don't have a dedicated setter function
export function useDirectStoreUpdater(
    key: keyof ITableState,
    value: unknown,
    setState: StoreApi<ITableState>['setState']
) {
    useEffect(() => {
        if (typeof value !== 'undefined') {
            setState({ [key]: value })
        }
    }, [value])
}

const globalGetId = (record: any) => val(record.id)

const StoreUpdater = ({
    rowSelectedIds,
    onColumnsChange,
    rows,
    data,
    isQueryInline,
    onIncludedRowsChange,
    onRowHighlightChange,
    columnTypes,
    records,
    getId = globalGetId,
    queryable,
    children,
}: StoreUpdaterProps) => {
    const {
        // setNodes,
        // setEdges,
        // setDefaultNodesAndEdges,
        // setMinZoom,
        // setMaxZoom,
        // setTranslateExtent,
        // setNodeExtent,
        // reset,
    } = useStore(selector, shallow)
    const store = useStoreApi()

    // useEffect(() => {
    //     const edgesWithDefaults = defaultEdges?.map((e) => ({ ...e, ...defaultEdgeOptions }))
    //     setDefaultNodesAndEdges(defaultNodes, edgesWithDefaults)

    //     return () => {
    //         reset()
    //     }
    // }, [])
    useDirectStoreUpdater('queryable', queryable, store.setState)

    useDirectStoreUpdater('rowSelectedIds', rowSelectedIds, store.setState)
    useDirectStoreUpdater('columnTypes', columnTypes, store.setState)
    useDirectStoreUpdater('records', records, store.setState)
    useDirectStoreUpdater('onColumnsChange', onColumnsChange, store.setState)
    useDirectStoreUpdater('rows', rows, store.setState)
    useDirectStoreUpdater('data', data, store.setState)
    useDirectStoreUpdater('isQueryInline', isQueryInline, store.setState)
    useDirectStoreUpdater('onIncludedRowsChange', onIncludedRowsChange, store.setState)
    useDirectStoreUpdater('onRowHighlightChange', onRowHighlightChange, store.setState)
    useDirectStoreUpdater('getId', getId, store.setState)

    // useStoreUpdater<Node[]>(nodes, setNodes)
    // useStoreUpdater(columns, store.setColumns)

    return children
}

export { StoreUpdater }

export default StoreUpdater
