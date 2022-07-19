import create, { StateCreator } from 'zustand'
import { devtools, persist, subscribeWithSelector } from 'zustand/middleware'
import produce from 'immer'
import { v4 as uuid } from 'uuid'
import { ConfigT } from './types'
import { FilterOperateSelectorValueT } from './filter-operate-selector'
import React from 'react'

// export interface ITableState {
//     isInit: boolean
//     views: ConfigT[]
//     defaultView: ConfigT
//     viewEditing: ConfigT
//     viewModelShow: boolean
//     onShowViewModel: (viewModelShow: boolean, viewEditing: ConfigT | null) => void
//     setViews: (views: ConfigT[]) => void
//     onViewAdd: (view: ConfigT) => void
//     onViewUpdate: (view: ConfigT) => void
//     getDefaultViewId: () => string
//     checkDuplicateViewName: (name: string, viewId: string) => boolean
//     currentView: ConfigT
//     onCurrentViewFiltersChange: (filters: FilterOperateSelectorValueT[]) => void
//     onCurrentViewColumnsChange: (selectedIds: any[], pinnedIds: any[], sortedIds: any[]) => void
// }
// eslint-disable-next-line prefer-template
const getId = (str: string) => str + '-' + uuid().substring(0, 8)

export interface ITableStateInitState {
    isInit: boolean
}
export interface IViewState {
    views: ConfigT[]
    defaultView: ConfigT
    setViews: (views: ConfigT[]) => void
    onViewAdd: (view: ConfigT) => void
    onViewUpdate: (view: ConfigT) => void
    getDefaultViewId: () => string
    checkDuplicateViewName: (name: string, viewId: string) => boolean
}
export interface ICurrentViewState {
    currentView: ConfigT
    onCurrentViewFiltersChange: (filters: FilterOperateSelectorValueT[]) => void
    onCurrentViewColumnsChange: (selectedIds: any[], pinnedIds: any[], sortedIds: any[]) => void
}
export interface IViewInteractiveState {
    viewEditing: ConfigT
    viewModelShow: boolean
    onShowViewModel: (viewModelShow: boolean, viewEditing: ConfigT | null) => void
}

export type ITableState = IViewState & ICurrentViewState & IViewInteractiveState & ITableStateInitState & IRowState

export type IStateCreator<T> = StateCreator<
    ITableState,
    [['zustand/subscribeWithSelector', never], ['zustand/devtools', never], ['zustand/persist', unknown]],
    [],
    T
>

const createViewSlice: IStateCreator<IViewState> = (set, get, store) => ({
    views: [],
    defaultView: {},
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
    checkDuplicateViewName: (name: string, viewId: string) => {
        return get()
            .views.filter((view) => view.id !== viewId)
            .some((view) => view.name === name)
    },
    getDefaultViewId: () => get().views?.find((view) => view.def)?.id ?? '',
})

const createCurrentViewSlice: IStateCreator<ICurrentViewState> = (set, get, store) => ({
    currentView: {},
    onCurrentViewFiltersChange: (filters) => set({ currentView: { ...get().currentView, filters } }),
    onCurrentViewColumnsChange: (selectedIds: any[], pinnedIds: any[], sortedIds: any[]) =>
        set({ currentView: { ...get().currentView, selectedIds, pinnedIds, sortedIds } }),
})

const createViewInteractiveSlice: IStateCreator<IViewInteractiveState> = (set, get, store) => ({
    viewEditing: {},
    viewModelShow: false,
    onShowViewModel: (viewModelShow, viewEditing) =>
        set(
            produce((state) => {
                // eslint-disable-next-line no-param-reassign
                state.viewEditing = viewEditing
                // eslint-disable-next-line no-param-reassign
                state.viewModelShow = viewModelShow
            })
        ),
})

const createTableStateInitSlice: IStateCreator<ITableStateInitState> = (set, get, store) => ({
    isInit: false,
    key: 'table',
})

export interface IRowState {
    rowSelectedIds: Array<any>
    onSelectMany: (args: any[]) => void
    onSelectNone: () => void
    onSelectOne: (id: any) => void
}
const createRowSlice: IStateCreator<IRowState> = (set, get, store) => ({
    rowSelectedIds: [],
    onSelectMany: (incomingRows: any[]) =>
        set({
            rowSelectedIds: [...get().rowSelectedIds, ...incomingRows],
        }),
    onSelectNone: () =>
        set({
            rowSelectedIds: [],
        }),
    onSelectOne: (id: any) => {
        const selectedRowIds = new Set(get().rowSelectedIds)
        if (selectedRowIds.has(id)) {
            selectedRowIds.delete(id)
        } else {
            selectedRowIds.add(id)
        }
        set({
            rowSelectedIds: Array.from(selectedRowIds),
        })
    },
})

export function useCustomStore(key: string) {
    const initialized = React.useRef(null)
    if (initialized.current) return initialized.current
    const useStore = create<ITableState>()(
        subscribeWithSelector(
            devtools(
                persist(
                    (...a) => ({
                        ...createTableStateInitSlice(...a),
                        ...createViewSlice(...a),
                        ...createCurrentViewSlice(...a),
                        ...createViewInteractiveSlice(...a),
                        ...createRowSlice(...a),
                        key: `table/${key}`,
                    }),
                    {
                        name: `table/${key}`,
                    }
                )
            )
        )
    )
    useStore.subscribe(console.log)
    // TODO type define
    // @ts-ignore
    initialized.current = useStore
    return useStore
}
export type IStore = ReturnType<typeof useCustomStore>

// eslint-disable-next-line

export default useCustomStore
