import { useCallback } from 'react'
import { useStoreApi } from './useStore'
import { ConfigT } from '@starwhale/ui/base/data-table/types'

function useGridSave() {
    const store = useStoreApi().getState()

    const handleSave = async (view: ConfigT) => {
        if (!view.id || view.id === 'all') store.onShowViewModel(true, view)
        else {
            store.onViewUpdate(view)
            await store.onSave?.(view)
        }
    }

    const handleSaveAs = useCallback(
        (view) => {
            store.onShowViewModel(true, {
                ...view,
                id: undefined,
                updated: false,
            })
        },
        [store]
    )

    // changed status must be after all the store changes(after api success)
    const changed = store.currentView.updated

    return {
        onSave: handleSave,
        onSaveAs: handleSaveAs,
        changed,
    }
}

export { useGridSave }

export default useGridSave
