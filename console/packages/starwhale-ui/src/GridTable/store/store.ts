import create, { StateCreator, StoreApi, UseBoundStore } from 'zustand'
import { devtools, subscribeWithSelector, persist } from 'zustand/middleware'
import produce from 'immer'
import { v4 as uuid } from 'uuid'
import _ from 'lodash'
import { ConfigT, QueryT, SortDirectionsT } from '../../base/data-table/types'
import { FilterOperateSelectorValueT } from '../../base/data-table/filter-operate-selector'

// eslint-disable-next-line prefer-template
const getId = (str: string) => str + '-' + uuid().substring(0, 8)

export interface ITableStateInitState {
    isInit: boolean
    key: string
    initStore: (obj: Record<string, any>) => void
    setRawConfigs: (obj: Record<string, any>) => void
    getRawConfigs: (state?: ITableState) => typeof rawInitialState
    getRawIfChangedConfigs: (state?: ITableState) => typeof rawIfChangedInitialState
    reset: () => void
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
    onCurrentViewSaved: () => void
    onCurrentViewIdChange: (viewId?: string) => void
    onCurrentViewSort: (key: string, direction: SortDirectionsT) => void
    onCurrentViewFiltersChange: (filters: FilterOperateSelectorValueT[]) => void
    onCurrentViewQueriesChange: (queries: QueryT[]) => void
    onCurrentViewColumnsChange: (selectedIds: any[], pinnedIds: any[], ids: any[]) => void
    onCurrentViewColumnsPin: (columnId: string, bool?: boolean) => void
}
export interface IViewInteractiveState {
    viewEditing: ConfigT
    viewModelShow: boolean
    onShowViewModel: (viewModelShow: boolean, viewEditing: ConfigT | null) => void
}
export type ITableState = IViewState &
    ICurrentViewState &
    IViewInteractiveState &
    ITableStateInitState &
    IRowState &
    ICompareState

// , ['zustand/persist', unknown]
export type IStateCreator<T> = StateCreator<
    ITableState,
    [['zustand/subscribeWithSelector', never], ['zustand/devtools', never]],
    [],
    T
>

const rawInitialState: Partial<ITableState> = {
    key: 'table',
    views: [],
    defaultView: {},
    currentView: { id: 'all' },
    rowSelectedIds: [],
}

const rawIfChangedInitialState: Partial<ITableState> = {
    key: 'table',
    views: [],
}

// eslint-disable-next-line @typescript-eslint/no-unused-vars
const createViewSlice: IStateCreator<IViewState> = (set, get, store) => {
    const update = (updateAttrs: any, name?: string) => {
        const state = get()
        set(updateAttrs, undefined, name)
        // @FIXME state type for onViewsChange
        // @ts-ignore
        store.getState().onViewsChange?.(get(), state)
    }

    return {
        views: [],
        defaultView: {},
        setViews: (views) => {
            update(
                produce((state: ITableState) => {
                    // eslint-disable-next-line no-param-reassign
                    state.views = views
                    // eslint-disable-next-line no-param-reassign
                    state.currentView = views.find((view) => view.def) || {}
                })
            )
        },
        onViewAdd: (view) => update({ views: [...get().views, view] }, 'onViewAdd'),
        onViewUpdate: (view) => {
            // eslint-disable-next-line no-param-reassign
            view.updated = false
            // eslint-disable-next-line no-param-reassign
            view.updateColumn = false
            // eslint-disable-next-line no-param-reassign
            view.version = 0
            //
            const $oldViewIndex = get().views?.findIndex((v) => v.id === view.id)

            // console.log($oldViewIndex, get().currentView.id, view.id, view.def)
            // create
            if ($oldViewIndex > -1) {
                update(
                    produce((state: ITableState) => {
                        // eslint-disable-next-line no-param-reassign
                        state.views[$oldViewIndex] = view

                        // edit default view and default == current so replace it && view.def === true
                        if (get().currentView?.id === view.id) {
                            // eslint-disable-next-line no-param-reassign
                            state.currentView = view
                        }
                    }),
                    'onViewUpdate'
                )
            } else {
                const $views = get().views?.map((v) => ({
                    ...v,
                    def: false,
                }))
                update(
                    produce((state: ITableState) => {
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
                    }),
                    'onViewUpdate'
                )
            }
        },
        checkDuplicateViewName: (name: string, viewId: string) => {
            return get()
                .views.filter((view) => view.id !== viewId)
                .some((view) => view.name === name)
        },
        getDefaultViewId: () => get().views?.find((view) => view.def)?.id ?? '',
    }
}

const rawCurrentView = {
    filters: [],
    ids: [],
    pinnedIds: [],
    selectedIds: [],
    sortBy: '',
    version: 0,
    updated: false,
    updateColumn: false,
    id: 'all',
}
const createCurrentViewSlice: IStateCreator<ICurrentViewState> = (set, get, store) => {
    const update = (updateAttrs: Partial<ConfigT>, name?: string) => {
        const prev = get()
        const curr = get().currentView
        const version = _.isNumber(curr.version) ? curr.version + 1 : 1
        set(
            {
                currentView: {
                    ...curr,
                    ...updateAttrs,
                    updated: true,
                    version,
                },
            },
            undefined,
            name
        )
        // @ts-ignore
        store.getState().onCurrentViewChange?.(get(), prev)
    }

    return {
        currentView: rawCurrentView,
        onCurrentViewSaved: () => update({ updated: false }, 'onCurrentViewSaved'),
        onCurrentViewIdChange: (viewId) => {
            if (viewId === 'all') {
                update(rawCurrentView, 'onCurrentViewIdChange')
                return
            }
            let view = get().views.find((v) => v.id === viewId)
            if (!view) {
                view = get().views.find((v) => v.def)
            }
            if (!view) {
                view = rawCurrentView
            }
            update(view, 'onCurrentViewIdChange')
        },
        onCurrentViewFiltersChange: (filters) => update({ filters }),
        onCurrentViewQueriesChange: (queries) => update({ queries }),
        onCurrentViewColumnsChange: (selectedIds: any[], pinnedIds: any[], ids: any[]) =>
            update({ selectedIds, pinnedIds, ids, updateColumn: true }),
        onCurrentViewColumnsPin: (columnId: string, pined = false) => {
            const { pinnedIds = [], ids = [] } = get().currentView
            const $pinnedIds = new Set(pinnedIds)
            if (pined) {
                $pinnedIds.add(columnId)
            } else {
                $pinnedIds.delete(columnId)
            }
            const sortedMergeSelectedIds = Array.from(ids).sort((v1, v2) => {
                const index1 = $pinnedIds.has(v1) ? 1 : -1
                const index2 = $pinnedIds.has(v2) ? 1 : -1
                return index2 - index1
            })
            update({ pinnedIds: Array.from($pinnedIds), ids: sortedMergeSelectedIds }, 'onCurrentViewColumnsPin')
        },
        onCurrentViewSort: (key, direction) => update({ sortBy: key, sortDirection: direction }, 'onCurrentViewSort'),
    }
}

// eslint-disable-next-line @typescript-eslint/no-unused-vars
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
            }),
            undefined,
            'onShowViewModel'
        ),
})

// eslint-disable-next-line @typescript-eslint/no-unused-vars
const createTableStateInitSlice: IStateCreator<ITableStateInitState> = (set, get, store) => ({
    isInit: false,
    key: 'table',
    initStore: (obj?: Record<string, any>) =>
        set(
            {
                ...(obj ? _.pick(obj, Object.keys(rawInitialState)) : rawInitialState),
                isInit: true,
            },
            undefined,
            'initStore'
        ),
    setRawConfigs: (obj: Record<string, any>) =>
        set(
            {
                ..._.pick(obj, Object.keys(rawInitialState)),
            },
            undefined,
            'setRawConfigs'
        ),
    getRawConfigs: (state) => _.pick(state ?? get(), Object.keys(rawInitialState)),
    getRawIfChangedConfigs: (state) => _.pick(state ?? get(), Object.keys(rawIfChangedInitialState)),
    reset: () => set(rawInitialState, undefined, 'reset'),
})

export interface IRowState {
    rowSelectedIds: Array<any>
    onSelectMany: (args: any[]) => void
    onSelectNone: () => void
    onSelectOne: (id: any) => void
    onNoSelect: (id: any) => void
    setRowSelectedIds: (ids: any[]) => void
}

// eslint-disable-next-line @typescript-eslint/no-unused-vars
const createRowSlice: IStateCreator<IRowState> = (set, get, store) => {
    const update = (updateAttrs: Partial<IRowState>, name?: string) => {
        set(updateAttrs, undefined, name)
        // @FIXME state type for onViewsChange
        // @ts-ignore
        store.getState().onRowSelectedChange?.(updateAttrs.rowSelectedIds)
    }

    return {
        rowSelectedIds: [],
        onSelectMany: (incomingRows: any[]) =>
            update(
                {
                    rowSelectedIds: [...incomingRows],
                },
                'onSelectMany'
            ),
        onSelectNone: () =>
            update(
                {
                    rowSelectedIds: [],
                },
                'onSelectNone'
            ),
        onNoSelect: (id: any) => {
            const selectedRowIds = new Set(get().rowSelectedIds)
            selectedRowIds.delete(id)
            update(
                {
                    rowSelectedIds: Array.from(selectedRowIds),
                },
                'onNoSelect'
            )
        },
        onSelectOne: (id: any) => {
            const selectedRowIds = new Set(get().rowSelectedIds)
            if (selectedRowIds.has(id)) {
                selectedRowIds.delete(id)
            } else {
                selectedRowIds.add(id)
            }
            update(
                {
                    rowSelectedIds: Array.from(selectedRowIds),
                },
                'onSelectOne'
            )
        },
        setRowSelectedIds: (rowSelectedIds: any[]) =>
            update(
                {
                    rowSelectedIds,
                },
                'setRowSelectedIds'
            ),
    }
}

export interface ICompareState {
    compare?: {
        comparePinnedKey: any
        compareShowCellChanges: boolean
        compareShowDiffOnly: boolean
    }
    onCompareUpdate: (args: any) => void
}

// eslint-disable-next-line @typescript-eslint/no-unused-vars
const createCompareSlice: IStateCreator<ICompareState> = (set, get, store) => ({
    onCompareUpdate: (args) =>
        set(
            {
                compare: {
                    ...get().compare,
                    ...args,
                },
            },
            false,
            'onCompareUpdate'
        ),
})

export function createCustomStore(key: string, initState: Partial<ITableState> = rawInitialState, isPersist = false) {
    const name = `table/${key}`
    //
    const actions: StateCreator<ITableState> = (...a: any) => ({
        // @ts-ignore
        ...createTableStateInitSlice(...a),
        // @ts-ignore
        ...createViewSlice(...a),
        // @ts-ignore
        ...createCurrentViewSlice(...a),
        // @ts-ignore
        ...createViewInteractiveSlice(...a),
        // @ts-ignore
        ...createRowSlice(...a),
        // @ts-ignore
        ...createCompareSlice(...a),
        ...initState,
        key: name,
    })
    if (isPersist) {
        return create<ITableState>()(subscribeWithSelector(devtools(persist(actions as any, { name }))))
    }
    const useStore = create<ITableState>()(actions)
    return useStore as UseBoundStore<StoreApi<ITableState>>
}
export type IStore = ReturnType<typeof createCustomStore>

export default createCustomStore

export const useEvaluationStore = createCustomStore('evaluations', {}, true)
export const useEvaluationCompareStore = createCustomStore('compare', {
    compare: {
        comparePinnedKey: '',
        compareShowCellChanges: true,
        compareShowDiffOnly: false,
    },
})
export const useEvaluationDetailStore = createCustomStore(
    'evaluations-detail',
    {
        compare: {
            comparePinnedKey: '',
            compareShowCellChanges: true,
            compareShowDiffOnly: false,
        },
    },
    false
)
const stateSelector = (state: ITableState) => state
const currentQueriesSelector = (state: ITableState) => state.currentView?.queries ?? []
const currentViewSelector = (state: ITableState) => state.currentView ?? {}

export { stateSelector, currentQueriesSelector, currentViewSelector }
