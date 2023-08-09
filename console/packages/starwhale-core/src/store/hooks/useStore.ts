import { useContext, useMemo } from 'react'
import { useStore as useZustandStore } from 'zustand'
import type { StoreApi } from 'zustand'

import { EditorContext } from '../../context'
import { WidgetStoreState } from '../../types'

const zustandErrorMessage = 'Could not find zustand store context value.'

type ExtractState = StoreApi<WidgetStoreState> extends { getState: () => infer T } ? T : never

function useStore<StateSlice = ExtractState>(
    selector: (state: WidgetStoreState) => StateSlice,
    equalityFn?: (a: StateSlice, b: StateSlice) => boolean
) {
    const { store } = useContext(EditorContext)

    if (store === null) {
        throw new Error(zustandErrorMessage)
    }

    return useZustandStore(store, selector, equalityFn)
}

const useStoreApi = () => {
    const { store } = useContext(EditorContext)

    if (store === null) {
        throw new Error(zustandErrorMessage)
    }

    return useMemo(
        () => ({
            getState: store.getState,
            setState: store.setState,
            subscribe: store.subscribe,
            destroy: store.destroy,
        }),
        [store]
    )
}

export { useStore, useStoreApi }
