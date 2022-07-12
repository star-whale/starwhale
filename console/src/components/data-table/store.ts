import create from 'zustand'
import { devtools, persist } from 'zustand/middleware'
import produce from 'immer'
import { v4 as uuid } from 'uuid'
import { ConfigT } from './types'
import { FilterOperateSelectorValueT } from './filter-operate-selector'

export interface ITableState {
    isInit: boolean
    views: ConfigT[]
    defaultView: ConfigT
    viewEditing: ConfigT
    viewModelShow: boolean
    onShowViewModel: (viewModelShow: boolean, viewEditing: ConfigT | null) => void
    setViews: (views: ConfigT[]) => void
    onViewAdd: (view: ConfigT) => void
    onViewUpdate: (view: ConfigT) => void
    getDefaultViewId: () => string
    checkDuplicateViewName: (name: string, viewId: string) => boolean
    currentView: ConfigT
    onCurrentViewFiltersChange: (filters: FilterOperateSelectorValueT[]) => void
    onCurrentViewColumnsChange: (selectedIds: any[], pinnedIds: any[], sortedIds: any[]) => void
}
// eslint-disable-next-line prefer-template
const getId = (str: string) => str + '-' + uuid().substring(0, 8)

const useStore = create<ITableState>()(
    devtools(
        persist(
            (set, get) => ({
                isInit: false,
                views: [],
                defaultView: {},
                viewEditing: {},
                viewModelShow: false,
                checkDuplicateViewName: (name: string, viewId: string) => {
                    return get()
                        .views.filter((view) => view.id !== viewId)
                        .some((view) => view.name === name)
                },
                onShowViewModel: (viewModelShow, viewEditing) =>
                    set(
                        produce((state) => {
                            // eslint-disable-next-line no-param-reassign
                            state.viewEditing = viewEditing
                            // eslint-disable-next-line no-param-reassign
                            state.viewModelShow = viewModelShow
                        })
                    ),
                setViews: (views) =>
                    set(
                        produce((state) => {
                            // eslint-disable-next-line no-param-reassign
                            state.views = views
                            // eslint-disable-next-line no-param-reassign
                            state.currentView = views.find((view) => view.def) || {}
                        })
                    ),
                onViewAdd: (view) => set({ views: [...get().views, view] }),
                onViewUpdate: (view) => {
                    const $oldViewIndex = get().views?.findIndex((v) => v.id === view.id)

                    // console.log($oldViewIndex, get().currentView.id, view.id, view.def)
                    // create
                    if ($oldViewIndex > -1) {
                        set(
                            produce((state) => {
                                // eslint-disable-next-line no-param-reassign
                                state.views[$oldViewIndex] = view

                                // edit default view and default == current so replace it && view.def === true
                                if (get().currentView?.id === view.id) {
                                    // eslint-disable-next-line no-param-reassign
                                    state.currentView = view
                                }
                            })
                        )
                    } else {
                        const $views = get().views?.map((v) => ({
                            ...v,
                            def: false,
                        }))
                        set(
                            produce((state) => {
                                const newView = {
                                    ...view,
                                    def: true,
                                    isShow: true,
                                    id: getId('view'),
                                }
                                // eslint-disable-next-line no-param-reassign
                                state.views = [...$views, newView]
                                // eslint-disable-next-line no-param-reassign
                                state.currentView = newView
                            })
                        )
                    }
                },
                currentView: {},
                onCurrentViewFiltersChange: (filters) => set({ currentView: { ...get().currentView, filters } }),
                onCurrentViewColumnsChange: (selectedIds: any[], pinnedIds: any[], sortedIds: any[]) =>
                    set({ currentView: { ...get().currentView, selectedIds, pinnedIds, sortedIds } }),
                getDefaultViewId: () => get().views?.find((view) => view.def)?.id ?? '',
            }),
            {
                name: 'table/evaluations',
            }
        )
    )
)
// eslint-disable-next-line
useStore.subscribe(console.log)

export default useStore

// useStore.subscribe(console.log)
// setViews: (views) =>
//     set(
//         produce((state) => {
//             state.views = views
//         })
//     ),
// }),
// getStorage: () => sessionStorage,
