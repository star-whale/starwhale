import { createContext } from 'react'

import { IGridState } from '../types'
import { UseBoundStore, StoreApi } from 'zustand'

const StoreContext = createContext<UseBoundStore<StoreApi<IGridState>> | null>(null)

export const { Provider } = StoreContext

export default StoreContext
