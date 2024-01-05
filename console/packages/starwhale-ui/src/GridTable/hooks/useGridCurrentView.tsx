import React from 'react'
import { ConfigT } from '../../base/data-table/types'
import { useStore } from '@starwhale/ui/GridTable/hooks/useStore'
import { ITableState } from '../store'

const selector = (state: ITableState) => ({
    currentView: state.currentView,
    views: state.views,
    columns: state.columns ?? [],
})

function useGridCurrentView() {
    const { currentView: view, columns } = useStore(selector)

    const columnIds = React.useMemo(() => {
        return columns.map((c) => c.key)
    }, [columns])

    const $ids = React.useMemo(() => {
        const { pinnedIds = [], ids = [] }: ConfigT = view

        // NOTICE: used full columns when
        if (!view.id || (view.id === 'all' && !view.updateColumn)) {
            return Array.from(new Set([...pinnedIds, ...columnIds]))
        }

        return ids
    }, [view, columnIds])

    const $view = React.useMemo(() => {
        return {
            ...view,
            ids: $ids,
        }
    }, [view, $ids])

    const isAllRuns = React.useMemo(() => {
        return view.id === 'all'
    }, [view])

    return {
        ids: $ids,
        currentView: $view,
        isAllRuns,
    }
}

export { useGridCurrentView }

export default useGridCurrentView
