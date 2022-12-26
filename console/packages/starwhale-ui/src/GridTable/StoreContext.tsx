import React, { useContext, createContext } from 'react'
import { useStore } from 'zustand'
import { createCustomStore, ITableState, IStore } from '../base/data-table/store'

type IStoreProviderProps = {
    storeKey?: string
    initState: Partial<ITableState>
    children: React.ReactNode
}

const TableContext = createContext<IStore | null>(null)

export function StoreProvider({ storeKey = 'store', initState, children }: IStoreProviderProps) {
    const storeRef = React.useRef<IStore>()
    if (!storeRef.current) {
        storeRef.current = createCustomStore(storeKey, initState)
    }
    return <TableContext.Provider value={storeRef.current}>{children}</TableContext.Provider>
}

export function useContextStore<T>(selector: (state: ITableState) => T) {
    const store = useContext(TableContext)
    if (store === null) {
        throw new Error('Missing Wrapper in the tree')
    }
    const value = useStore(store, selector)
    return value
}

export const useTableContext = () => {
    const store = useContext(TableContext)
    if (store === null) {
        throw new Error('Missing Wrapper in the tree')
    }
    return store as IStore
}
