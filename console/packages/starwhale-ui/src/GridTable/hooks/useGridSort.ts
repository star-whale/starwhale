import { useMemo } from 'react'
import useGridData from './useGridData'

function useGridSave() {
    const { columns, currentView } = useGridData()

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
