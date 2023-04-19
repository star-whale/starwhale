import { Operators } from '@starwhale/ui/base/data-table/filter-operate-selector'
import React, { useCallback } from 'react'
import { useStoreApi } from './useStore'

function useGridFilter(props) {
    const store = useStoreApi().getState()

    const $filters = React.useMemo(() => {
        return (
            props.initialFilters?.map((v) => ({
                ...v,
                op: Operators[v.op?.key || 'default'],
            })) || []
        )
    }, [props.initialFilters])

    const $filtersEnabled = React.useMemo(() => {
        return $filters?.filter((c) => !c.disable)
    }, [$filters])

    const handeFilterSet = useCallback(
        (categories) => {
            store.onCurrentViewFiltersChange(categories)
        },
        [store]
    )
    return {
        filters: $filters,
        filtersEnabled: $filtersEnabled,
        handeFilterSet,
    }
}

export { useGridFilter }

export default useGridFilter
