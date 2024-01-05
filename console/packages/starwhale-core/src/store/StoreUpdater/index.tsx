import { useEffect } from 'react'
import { StoreApi } from 'zustand'
import { useStoreApi } from '../hooks/useStore'
import { WidgetStateT, WidgetStoreState } from '@starwhale/core/types'

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

// eslint-disable-next-line @typescript-eslint/no-unused-vars
const selector = (s: WidgetStoreState) => ({
    // initState: s.initState,
})

const StoreUpdater = ({
    onStateChange,
    editable,
    panelGroup,
    onEvalSectionDelete,
    onSave,
    initialState,
    onInit,
}: StoreUpdaterProps) => {
    const store = useStoreApi()

    useDirectStoreUpdater('editable', editable, store.setState)
    useDirectStoreUpdater('panelGroup', panelGroup, store.setState)
    useDirectStoreUpdater('onStateChange', onStateChange, store.setState)
    // positive: for eval
    useDirectStoreUpdater('onEvalSectionDelete', onEvalSectionDelete, store.setState)
    useDirectStoreUpdater('onSave', onSave, store.setState)
    useDirectStoreUpdater('initialState', initialState, store.setState)
    // init
    useDirectStoreUpdater('onInit', onInit, store.setState)

    return null
}

export { StoreUpdater }

export default StoreUpdater
