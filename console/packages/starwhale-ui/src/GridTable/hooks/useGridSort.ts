import { shallow } from 'zustand/shallow'
import { useStore, useStoreApi } from './useStore'
import { ITableState } from '../store'

const selector = (state: ITableState) => ({
    sortIndex: state.sortIndex,
    sortDirection: state.sortDirection ?? [],
})

function useGridSort() {
    return useStore(selector, shallow)
}

export { useGridSort }

export default useGridSort
