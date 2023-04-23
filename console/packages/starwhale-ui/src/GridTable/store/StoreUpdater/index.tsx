import { useEffect } from 'react'
import { StoreApi } from 'zustand'
import { useStore, useStoreApi } from '../../hooks/useStore'
import shallow from 'zustand/shallow'
import { val } from '../../utils'
import { IGridState, ITableProps } from '../../types'
import { ITableState } from '../store'

type StoreUpdaterProps = ITableProps

export function useStoreUpdater<T>(value: T | undefined, setStoreState: (param: T) => void) {
    useEffect(() => {
        if (typeof value !== 'undefined') {
            setStoreState(value)
        }
        // eslint-disable-next-line react-hooks/exhaustive-deps
    }, [value])
}

// updates with values in store that don't have a dedicated setter function
export function useDirectStoreUpdater(
    key: keyof IGridState,
    value: unknown,
    setState: StoreApi<IGridState>['setState']
) {
    useEffect(() => {
        if (typeof value !== 'undefined') {
            // eslint-disable-next-line no-console
            console.log('set state', key)
            setState({ [key]: value }, false)
        }
        // eslint-disable-next-line react-hooks/exhaustive-deps
    }, [value])
}

const globalGetId = (record: any) => val(record.id)

const selector = (s: ITableState) => ({
    reset: s.reset,
})

const StoreUpdater = ({
    rowSelectedIds,
    onColumnsChange,
    rows,
    queryinline,
    onViewsChange,
    onCurrentViewChange,
    onSave,
    columnTypes,
    records,
    getId = globalGetId,
    queryable,
    onIncludedRowsChange,
    onRowHighlightChange,
    onRowSelectedChange,
}: StoreUpdaterProps) => {
    const { reset } = useStore(selector, shallow)
    const store = useStoreApi()

    useEffect(() => {
        return () => {
            // reset()
        }
    }, [reset])

    useDirectStoreUpdater('queryable', queryable, store.setState)
    useDirectStoreUpdater('onViewsChange', onViewsChange, store.setState)
    useDirectStoreUpdater('onCurrentViewChange', onCurrentViewChange, store.setState)
    useDirectStoreUpdater('onColumnsChange', onColumnsChange, store.setState)
    useDirectStoreUpdater('onSave', onSave, store.setState)
    useDirectStoreUpdater('rowSelectedIds', rowSelectedIds, store.setState)
    useDirectStoreUpdater('columnTypes', columnTypes, store.setState)
    useDirectStoreUpdater('records', records, store.setState)
    useDirectStoreUpdater('rows', rows, store.setState)
    useDirectStoreUpdater('queryinline', queryinline, store.setState)
    useDirectStoreUpdater('getId', getId, store.setState)
    useDirectStoreUpdater('onIncludedRowsChange', onIncludedRowsChange, store.setState)
    useDirectStoreUpdater('onRowHighlightChange', onRowHighlightChange, store.setState)
    useDirectStoreUpdater('onRowSelectedChange', onRowSelectedChange, store.setState)

    // useStoreUpdater<Node[]>(nodes, setNodes)
    // useStoreUpdater(columns, store.setColumns)
    return null
}

export { StoreUpdater }

export default StoreUpdater
