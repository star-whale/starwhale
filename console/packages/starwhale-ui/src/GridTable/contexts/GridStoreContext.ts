import { createContext } from 'react'

import { createCustomStore } from '../../base/data-table/store'

const StoreContext = createContext<ReturnType<typeof createCustomStore> | null>(null)

export const { Provider } = StoreContext

export default StoreContext
