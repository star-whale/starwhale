import create, { StateCreator, StoreApi, UseBoundStore } from 'zustand'
import { devtools, subscribeWithSelector, persist } from 'zustand/middleware'
import produce from 'immer'
import { v4 as uuid } from 'uuid'
import _ from 'lodash'
import { ColumnT, ConfigT, QueryT, SortDirectionsT } from '../../base/data-table/types'
import { FilterOperateSelectorValueT } from '../../base/data-table/filter-operate-selector'

// eslint-disable-next-line prefer-template
const getId = (str: string) => str + '-' + uuid().substring(0, 8)

export type ITableProps = {
    columns: ColumnT[]
}

export interface ITableStateInitState {
    isInit: boolean
    key: string
    initStore: (obj: Record<string, any>) => void
    setRawConfigs: (obj: Record<string, any>) => void
    getRawConfigs: (state?: ITableState) => typeof rawInitialState
    getRawIfChangedConfigs: (state?: ITableState) => typeof rawIfChangedInitialState
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
export type ITableState = ITableProps &
    IViewState &
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
        //
        view.updated = false
        view.updateColumn = false
        view.version = 0
        //
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
const createCurrentViewSlice: IStateCreator<ICurrentViewState> = (set, get) => {
    const update = (updateAttrs: Partial<ConfigT>) => {
        const curr = get().currentView
        const version = _.isNumber(curr.version) ? curr.version + 1 : 1
        set({
            currentView: {
                ...curr,
                ...updateAttrs,
                updated: true,
                version,
            },
        })
    }

    return {
        currentView: rawCurrentView,
        onCurrentViewSaved: () => update({ updated: false }),
        onCurrentViewIdChange: (viewId) => {
            if (viewId === 'all') {
                set({ currentView: rawCurrentView })
                return
            }
            let view = get().views.find((view) => view.id === viewId)
            if (!view) {
                view = get().views.find((view) => view.def)
            }
            if (!view) {
                view = rawCurrentView
            }
            set({ currentView: view })
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
            update({ pinnedIds: Array.from($pinnedIds), ids: sortedMergeSelectedIds })
        },
        onCurrentViewSort: (key, direction) => update({ sortBy: key, sortDirection: direction }),
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
            })
        ),
})

// eslint-disable-next-line @typescript-eslint/no-unused-vars
const createTableStateInitSlice: IStateCreator<ITableStateInitState> = (set, get, store) => ({
    isInit: false,
    key: 'table',
    initStore: (obj?: Record<string, any>) =>
        set({
            ...(obj ? _.pick(obj, Object.keys(rawInitialState)) : rawInitialState),
            isInit: true,
        }),
    setRawConfigs: (obj: Record<string, any>) =>
        set({
            ..._.pick(obj, Object.keys(rawInitialState)),
        }),
    getRawConfigs: (state) => _.pick(state ?? get(), Object.keys(rawInitialState)),
    getRawIfChangedConfigs: (state) => _.pick(state ?? get(), Object.keys(rawIfChangedInitialState)),
})

export interface IRowState {
    rowSelectedIds: Array<any>
    onSelectMany: (args: any[]) => void
    onSelectNone: () => void
    onSelectOne: (id: any) => void
    onNoSelect: (id: any) => void
}

// eslint-disable-next-line @typescript-eslint/no-unused-vars
const createRowSlice: IStateCreator<IRowState> = (set, get, store) => ({
    rowSelectedIds: [],
    onSelectMany: (incomingRows: any[]) =>
        set({
            rowSelectedIds: [...incomingRows],
        }),
    onSelectNone: () =>
        set({
            rowSelectedIds: [],
        }),
    onNoSelect: (id: any) => {
        const selectedRowIds = new Set(get().rowSelectedIds)
        selectedRowIds.delete(id)
        set({
            rowSelectedIds: Array.from(selectedRowIds),
        })
    },
    onSelectOne: (id: any) => {
        const selectedRowIds = new Set(get().rowSelectedIds)
        if (selectedRowIds.has(id)) {
            selectedRowIds.delete(id)
        } else {
            selectedRowIds.add(id)
        }
        console.log(Array.from(selectedRowIds), '---------')
        set({
            rowSelectedIds: Array.from(selectedRowIds),
        })
    },
})

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
        set({
            compare: {
                ...get().compare,
                ...args,
            },
        }),
})

export function createCustomStore(key: string, initState: Partial<ITableState> = {}, isPersist = false) {
    let initialized = null
    if (initialized) return initialized
    const name = `table/${key}`
    const useStore = create<ITableState>()(
        subscribeWithSelector(
            devtools(
                isPersist
                    ? persist(
                          (...a) => ({
                              //   @ts-ignore
                              ...createTableStateInitSlice(...a),
                              //   @ts-ignore
                              ...createViewSlice(...a),
                              //   @ts-ignore
                              ...createCurrentViewSlice(...a),
                              //   @ts-ignore
                              ...createViewInteractiveSlice(...a),
                              //   @ts-ignore
                              ...createRowSlice(...a),
                              //   @ts-ignore
                              ...createCompareSlice(...a),
                              ...initState,
                              key: name,
                          }),
                          { name }
                      )
                    : (...a) => ({
                          ...createTableStateInitSlice(...a),
                          ...createViewSlice(...a),
                          ...createCurrentViewSlice(...a),
                          ...createViewInteractiveSlice(...a),
                          ...createRowSlice(...a),
                          ...createCompareSlice(...a),
                          ...initState,
                          key: name,
                      }),
                { name }
            )
        )
    )
    // eslint-disable-next-line
    // useStore.subscribe(console.log)
    // TODO type define
    // @ts-ignore
    initialized = useStore
    return useStore
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
    true
)
const stateSelector = (state: ITableState) => state
const currentQueriesSelector = (state: ITableState) => state.currentView?.queries ?? []
const currentViewSelector = (state: ITableState) => state.currentView ?? {}

export { stateSelector, currentQueriesSelector, currentViewSelector }
