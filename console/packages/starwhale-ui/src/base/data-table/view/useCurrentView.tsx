import _ from 'lodash'
import React from 'react'
import { currentViewSelector, IStore } from '../store'
import { ColumnT, ConfigT } from '../types'

function useConfigView(store: IStore, { columns }: { columns: ColumnT[] }) {
    const view = store(currentViewSelector)

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
    }, [view])

    const isAllRuns = React.useMemo(() => {
        return view.id === 'all'
    }, [view])

    return {
        ids: $ids,
        columns: $columns,
        currentView: $view,
        isAllRuns,
    }
}

export default useConfigView
