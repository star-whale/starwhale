import { useEffect } from 'react'
import { StoreApi } from 'zustand'
import { useStore, useStoreApi } from '../../hooks/useStore'
import shallow from 'zustand/shallow'
import { val } from '../../utils'
import { IGridState, ITableProps } from '../../types'
import { ITableState } from '../store'
import { ConfigT } from '@starwhale/ui/base/data-table/types'

type StoreUpdaterProps = ITableProps

export function useStoreEmptyUpdater<T>(value: T | undefined, setStoreState: (param: T) => void) {
    useEffect(() => {
        setStoreState(value as any)
        // eslint-disable-next-line react-hooks/exhaustive-deps
    }, [value])
}

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
            // console.log('set state', key)
            // @ts-ignore
            setState({ [key]: value }, false, `[updater] ${key}`)
        }
        // eslint-disable-next-line react-hooks/exhaustive-deps
    }, [value])
}

const globalGetId = (record: any) => val(record.id)

const selector = (s: ITableState) => ({
    reset: s.reset,
    setCurrentView: s.setCurrentView,
})

const StoreUpdater = ({
    rowSelectedIds,
    onColumnsChange,
    rows,
    queryinline,
    columnleinline,
    fillable,
    onViewsChange,
    currentView,
    onCurrentViewChange,
    onSave,
    columnTypes,
    records,
    getId = globalGetId,
    queryable,
    sortable = true,
    onIncludedRowsChange,
    onRowHighlightChange,
    onRowSelectedChange,
    page,
    onPageChange,
    onRemove,
}: StoreUpdaterProps) => {
    const { reset, setCurrentView } = useStore(selector, shallow)
    const store = useStoreApi()

    useEffect(() => {
        return () => {
            // reset()
        }
    }, [reset])

    // config
    useDirectStoreUpdater('sortable', sortable, store.setState)
    useDirectStoreUpdater('fillable', fillable, store.setState)
    useDirectStoreUpdater('queryable', queryable, store.setState)
    useDirectStoreUpdater('queryinline', queryinline, store.setState)
    useDirectStoreUpdater('columnleinline', columnleinline, store.setState)
    // fn
    useDirectStoreUpdater('getId', getId, store.setState)
    useDirectStoreUpdater('onSave', onSave, store.setState)
    useDirectStoreUpdater('onRemove', onRemove, store.setState)
    useDirectStoreUpdater('onPageChange', onPageChange, store.setState)
    useDirectStoreUpdater('onViewsChange', onViewsChange, store.setState)
    useDirectStoreUpdater('onColumnsChange', onColumnsChange, store.setState)
    useDirectStoreUpdater('onCurrentViewChange', onCurrentViewChange, store.setState)
    useDirectStoreUpdater('onIncludedRowsChange', onIncludedRowsChange, store.setState)
    useDirectStoreUpdater('onRowHighlightChange', onRowHighlightChange, store.setState)
    useDirectStoreUpdater('onRowSelectedChange', onRowSelectedChange, store.setState)
    // data
    useDirectStoreUpdater('page', page, store.setState)
    useDirectStoreUpdater('rows', rows, store.setState)
    useDirectStoreUpdater('records', records, store.setState)
    useDirectStoreUpdater('columnTypes', columnTypes, store.setState)
    useDirectStoreUpdater('rowSelectedIds', rowSelectedIds, store.setState)

    useStoreEmptyUpdater<ConfigT>(currentView, setCurrentView)
    return null
}

export { StoreUpdater }

export default StoreUpdater
