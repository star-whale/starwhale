import React, { useCallback, useMemo } from 'react'
import { useStoreApi } from './useStore'

const selector = (state) => ({
    currentView: state.currentView,
    columns: state.columns,
})

function useGridSave() {
    const { currentView, columns } = useStoreApi(selector)
    const [$sortIndex, $sortDirection] = useMemo(() => {
        const { sortBy, sortDirection } = currentView || {}
        const sortIndex = columns.findIndex((c) => c.key === sortBy)
        return [sortIndex, sortDirection]
    }, [currentView, columns])

    return {
        sortIndex: $sortIndex,
        sortDirection: $sortDirection,
    }
}

export { useGridSave }

export default useGridSave
