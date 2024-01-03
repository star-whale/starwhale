import { useEffect } from 'react'
import { StoreApi } from 'zustand'
import { useStore, useStoreApi } from '../../hooks/useStore'
import { shallow } from 'zustand/shallow'
import { val } from '../../utils'
import { IGridState, ITableProps } from '../../types'
import { ITableState } from '../store'
import { ConfigT } from '@starwhale/ui/base/data-table/types'
import { useDatastoreColumns } from '@starwhale/ui/GridDatastoreTable'
import { useTrace } from '@starwhale/core'

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
    columnHints,
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
    removable,
    selectable,
    columns,
}: StoreUpdaterProps) => {
    const { reset, setCurrentView } = useStore(selector, shallow)
    const store = useStoreApi()

    useEffect(() => {
        return () => {
            // reset()
        }
    }, [reset])

    const trace = useTrace('store-updater')

    const $columns = useDatastoreColumns({
        fillWidth: !!fillable,
        columnTypes,
        columnHints,
    })

    trace('-- GridTable StoreUpdater --', { $columns, columnTypes, columnHints })

    // config
    useDirectStoreUpdater('sortable', sortable, store.setState)
    useDirectStoreUpdater('fillable', fillable, store.setState)
    useDirectStoreUpdater('queryable', queryable, store.setState)
    useDirectStoreUpdater('removable', removable, store.setState)
    useDirectStoreUpdater('selectable', selectable, store.setState)
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
    useDirectStoreUpdater('columns', $columns, store.setState)
    useDirectStoreUpdater('originalColumns', columns, store.setState)
    useDirectStoreUpdater('records', records, store.setState)
    useDirectStoreUpdater('columnTypes', columnTypes, store.setState)
    useDirectStoreUpdater('columnHints', columnHints, store.setState)
    useDirectStoreUpdater('rowSelectedIds', rowSelectedIds, store.setState)

    useStoreEmptyUpdater<ConfigT>(currentView, setCurrentView)
    return null
}

export { StoreUpdater }

export default StoreUpdater
