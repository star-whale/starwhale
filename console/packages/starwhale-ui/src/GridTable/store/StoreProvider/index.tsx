import React, { createContext } from 'react'
// import StoreContext from '../../contexts/GridStoreContext'
import createCustomStore, { IStore, ITableState } from '../store'

// import { IGridState } from '../types'

// import { UseBoundStore, StoreApi } from 'zustand'

type IStoreProviderProps = {
    store?: IStore
    storeKey?: string
    initState: Partial<ITableState>
    children: React.ReactNode
}

const StoreContext = createContext<IStore | null>(null)

export const { Provider } = StoreContext

export function StoreProvider({ storeKey = 'store', initState, store, children }: IStoreProviderProps) {
    const storeRef = React.useRef<IStore>()
    if (!storeRef.current) {
        storeRef.current = store ?? createCustomStore(storeKey, initState)
    }
    return <StoreContext.Provider value={storeRef.current}>{children}</StoreContext.Provider>
}

export default StoreContext
