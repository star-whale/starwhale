import { useMemo } from 'react'
import { useStore } from './useStore'
import { IGridState } from '../types'
import shallow from 'zustand/shallow'

const selector = (state: IGridState) => ({
    currentView: state.currentView,
    columns: state.columns,
})

function useGridSave() {
    const { currentView, columns } = useStore(selector, shallow)
    const [$sortIndex, $sortDirection] = useMemo(() => {
        const { sortBy, sortDirection } = currentView || {}
        const sortIndex = columns?.findIndex((c) => c.key === sortBy)
        return [sortIndex, sortDirection]
    }, [currentView, columns])

    return {
        sortIndex: $sortIndex,
        sortDirection: $sortDirection,
    }
}

export { useGridSave }

export default useGridSave
