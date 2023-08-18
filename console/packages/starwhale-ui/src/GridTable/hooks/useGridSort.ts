import { useMemo } from 'react'
import useGirdData from './useGridData'
import useGridCurrentView from './useGridCurrentView'

function useGridSort() {
    const { originalColumns } = useGirdData()
    const { columns, currentView } = useGridCurrentView(originalColumns)

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

export { useGridSort }

export default useGridSort
