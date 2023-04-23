import React from 'react'
import StoreContext from '../../contexts/GridStoreContext'
import createCustomStore, { IStore, ITableState } from '../store'

type IStoreProviderProps = {
    store?: IStore
    storeKey?: string
    initState: Partial<ITableState>
    children: React.ReactNode
}

export function StoreProvider({ storeKey = 'store', initState, store, children }: IStoreProviderProps) {
    const storeRef = React.useRef<IStore>()
    if (!storeRef.current) {
        storeRef.current = store ?? createCustomStore(storeKey, initState)
    }
    return <StoreContext.Provider value={storeRef.current}>{children}</StoreContext.Provider>
}
