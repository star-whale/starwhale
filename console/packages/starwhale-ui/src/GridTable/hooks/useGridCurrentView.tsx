import _ from 'lodash'
import React from 'react'
import { ColumnT, ConfigT } from '../../base/data-table/types'
import { useStore, useStoreApi } from '@starwhale/ui/GridTable/hooks/useStore'
import { ITableState, arrayOverride } from '../store'

const selector = (state: ITableState) => ({
    currentView: state.currentView,
    views: state.views,
})

function useGridCurrentView(columns: ColumnT[]) {
    const { currentView: view } = useStore(selector)
    const store = useStoreApi()

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

    const $columns = React.useMemo(() => {
        const { pinnedIds = [] }: ConfigT = view
        const columnsMap = _.keyBy(columns, (c) => c.key) as Record<string, ColumnT>
        return $ids
            .filter((id: any) => id in columnsMap)
            .map((id: any) => {
                return {
                    ...columnsMap[id],
                    pin: pinnedIds.includes(id) ? 'LEFT' : undefined,
                }
            }) as ColumnT[]
    }, [view, columns, $ids])

    const $view = React.useMemo(() => {
        return {
            ...view,
            ids: $ids,
        }
    }, [view, $ids])

    const isAllRuns = React.useMemo(() => {
        return view.id === 'all'
    }, [view])

    const setCurrentView = React.useCallback(
        (next: ConfigT) => {
            if ($view === next) return
            store.setState({
                currentView: _.mergeWith($view, next, arrayOverride),
            })
        },
        [store, $view]
    )

    return {
        ids: $ids,
        columns: $columns,
        currentView: $view,
        setCurrentView,
        isAllRuns,
    }
}

export { useGridCurrentView }

export default useGridCurrentView
