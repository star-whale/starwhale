import { useEffect } from 'react'
import { StoreApi } from 'zustand'
import { useStore, useStoreApi } from '../hooks/useStore'
import { WidgetStateT, WidgetStoreState } from '@starwhale/core/types'
import { useTrace } from '@starwhale/core/utils'
import { useDatastoreColumns } from '@starwhale/ui/GridDatastoreTable'
import { shallow } from 'zustand/shallow'

type StoreUpdaterProps = Pick<WidgetStoreState, 'panelGroup' | 'editable'> & {
    onStateChange?: (param: WidgetStateT) => void
    initialState?: any
    onSave?: (state: WidgetStateT) => void
    onEvalSectionDelete?: () => void
    onInit?: ({ store }) => void
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
    key: keyof WidgetStoreState,
    value: unknown,
    setState: StoreApi<WidgetStoreState>['setState']
) {
    useEffect(() => {
        if (typeof value !== 'undefined') {
            // eslint-disable-next-line no-console
            // console.log('set state', key)
            setState({ [key]: value }, false)
        }
        // eslint-disable-next-line react-hooks/exhaustive-deps
    }, [value])
}

const useStoreComputeUpdater = (
    deps,
    fn: (selectedState: WidgetStoreState, previousSelectedState: WidgetStoreState) => void,
    subscribe
) => {
    useEffect(() => {
        const unsub = subscribe(
            (state) => deps.map((dep) => (typeof dep === 'function' ? dep(state) : state[dep])),
            // @ts-ignore
            fn,
            { equalityFn: shallow }
        )

        return () => {
            // reset()
            unsub()
        }
    }, [])

    return subscribe
}

// eslint-disable-next-line @typescript-eslint/no-unused-vars
const selector = (s: WidgetStoreState) => ({
    // initState: s.initState,
    computeColumns: s.computeColumns,
    computeSortIndex: s.computeSortIndex,
    computeRows: s.computeRows,
})

const StoreUpdater = ({
    onStateChange,
    editable,
    panelGroup,
    onEvalSectionDelete,
    onSave,
    initialState,
    onInit,
    rows,
    columns,
    wrapperRef,
    fillable,
    columnTypes,
    columnHints,
    records,
    page,
}: StoreUpdaterProps) => {
    const { computeColumns, computeSortIndex, computeRows } = useStore(selector, shallow)

    const store = useStoreApi()

    const trace = useTrace('core-store-updater')

    const $columns = useDatastoreColumns({
        fillWidth: !!fillable,
        columnTypes,
        columnHints,
    })

    trace('-- Store StoreUpdater --', { $columns, columnTypes, columnHints })

    useDirectStoreUpdater('wrapperRef', wrapperRef, store.setState)

    useDirectStoreUpdater('editable', editable, store.setState)
    useDirectStoreUpdater('panelGroup', panelGroup, store.setState)
    useDirectStoreUpdater('onStateChange', onStateChange, store.setState)
    // positive: for eval
    useDirectStoreUpdater('onEvalSectionDelete', onEvalSectionDelete, store.setState)
    useDirectStoreUpdater('onSave', onSave, store.setState)
    useDirectStoreUpdater('initialState', initialState, store.setState)
    // init
    useDirectStoreUpdater('onInit', onInit, store.setState)
    // data
    useDirectStoreUpdater('page', page, store.setState)
    useDirectStoreUpdater('columnTypes', columnTypes, store.setState)
    useDirectStoreUpdater('columnHints', columnHints, store.setState)
    // columns
    useDirectStoreUpdater('columns', $columns, store.setState)
    useDirectStoreUpdater('originalColumns', columns, store.setState)
    useStoreComputeUpdater(['columns', 'currentView'], computeColumns, store.subscribe)
    useStoreComputeUpdater(['columns', (s) => s.currentView?.sortBy], computeSortIndex, store.subscribe)
    // rows
    useDirectStoreUpdater('rows', rows, store.setState)
    useDirectStoreUpdater('records', records, store.setState)
    useStoreComputeUpdater(['records'], computeRows, store.subscribe)

    return null
}

export { StoreUpdater }

export default StoreUpdater
