import React, { useContext, createContext } from 'react'
import { useStore } from 'zustand'
import { createCustomStore, ITableState, IStore } from './store'

// const { Provider, useStore } = createContext()

type IStoreProviderProps = {
    key?: string
    initState: Partial<ITableState>
    children: React.ReactNode
}

// const StoreProvider = ({ key, initState, children }: IStoreProviderProps) => (
//     const storeRef = useRef<ReturnType<typeof createCustomStore>>();
//   if (!storeRef.current) {
//     storeRef.current = createMyStore();
//   }

//     <Provider createStore={() => createCustomStore(key ?? 'store', initState)}>{children}</Provider>
// )

// export { StoreProvider, useStore }

const MyContext = createContext<IStore | null>(null)

export function StoreProvider({ key = 'store', initState, children }: IStoreProviderProps) {
    const storeRef = React.useRef<IStore>()
    if (!storeRef.current) {
        storeRef.current = createCustomStore(key, initState)
    }
    // console.log(storeRef.current)
    return <MyContext.Provider value={storeRef.current}>{children}</MyContext.Provider>
}

export function useContextStore<T>(selector: (state: ITableState) => T) {
    const store = useContext(MyContext)
    if (store === null) {
        throw new Error('Missing Wrapper in the tree')
    }
    const value = useStore(store, selector)
    return value
}
