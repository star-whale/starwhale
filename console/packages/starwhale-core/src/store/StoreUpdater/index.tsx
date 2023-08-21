import { useEffect } from 'react'
import { StoreApi } from 'zustand'
import { useStoreApi } from '../hooks/useStore'
import { WidgetStateT, WidgetStoreState } from '@starwhale/core/types'

type StoreUpdaterProps = Pick<WidgetStoreState, 'panelGroup' | 'editable'> & {
    onStateChange?: (param: WidgetStateT) => void
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

const StoreUpdater = ({ onStateChange, editable, panelGroup }: StoreUpdaterProps) => {
    // eslint-disable-next-line @typescript-eslint/no-unused-vars
    // const { reset } = useStore(selector, shallow)
    const store = useStoreApi()

    // useEffect(() => {
    //     return () => {
    //         // reset()
    //     }
    // }, [reset])

    useDirectStoreUpdater('editable', editable, store.setState)
    useDirectStoreUpdater('panelGroup', panelGroup, store.setState)
    useDirectStoreUpdater('onStateChange', onStateChange, store.setState)
    return null
}

export { StoreUpdater }

export default StoreUpdater
