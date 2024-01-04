import { create, StateCreator, StoreApi, UseBoundStore } from 'zustand'
import { devtools, subscribeWithSelector, persist } from 'zustand/middleware'
import { v4 as uuid } from 'uuid'
import _ from 'lodash'
import { ConfigT, QueryT, SortDirectionsT } from '../../base/data-table/types'
import { FilterOperateSelectorValueT } from '../../base/data-table/filter-operate-selector'

// eslint-disable-next-line
export function arrayOverride(objValue: any, srcValue: any, key, object) {
    if (_.isArray(objValue)) {
        return srcValue
    }
    if (srcValue === null || srcValue === undefined) {
        _.unset(object, key)
    }
}

// eslint-disable-next-line prefer-template
const genViewId = (str: string) => str + '-' + uuid().substring(0, 8)

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
    setCurrentView: (view: ConfigT) => void
    onCurrentViewSaved: () => void
    onCurrentViewIdChange: (viewId?: string) => void
    onCurrentViewSort: (key: string, direction: SortDirectionsT) => void
    onCurrentViewFiltersChange: (filters: FilterOperateSelectorValueT[]) => void
    onCurrentViewQueriesChange: (queries: QueryT[]) => void
    onCurrentViewColumnsChange: (selectedIds: any[], pinnedIds: any[], ids: any[]) => void
    onCurrentViewColumnsPin: (columnId: string, bool?: boolean) => void
    onColumnResize: (columnKey: any, resizeDelta: number) => void
    getResizeDeltas: () => Record<string, any> | undefined
    setResizeDeltas: (resizeDeltas: Record<string, any>) => void
    getMeasuredWidths: () => Record<string, any> | undefined
    setMeasuredWidths: (measuredWidths: Record<string, any>) => void
}
export interface IViewInteractiveState {
    viewEditing: ConfigT
    viewModelShow: boolean
    onShowViewModel: (viewModelShow: boolean, viewEditing: ConfigT | null) => void
}
export interface IComputeState {
    columns: any[]
    rows: any[]
    sortIndex: number
    sortDirection: SortDirectionsT
}
export interface IUpdaterState {
    originalColumns: any[]
    columns: any[]
    records: any[]
    getId: (record: any) => any
}
export type ITableState = IViewState &
    ICurrentViewState &
    IViewInteractiveState &
    ITableStateInitState &
    IRowState &
    ICompareState & {
        compute: IComputeState
    } & IUpdaterState

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
            update({
                views,
                currentView: views.find((view) => view.def) || get().currentView,
            })
        },
        onViewAdd: (view) => update({ views: [...get().views, view] }, 'onViewAdd'),
        onViewUpdate: (view) => {
            const $view = { ...view, updated: false, updateColumn: false, version: 0 }
            const $views = [...get().views]
            const $oldViewIndex = $views?.findIndex((v) => v.id === $view.id)

            if ($oldViewIndex > -1) {
                $views[$oldViewIndex] = $view
                update(
                    {
                        views: $views,
                        currentView: get().currentView?.id === $view.id ? $view : get().currentView,
                    },
                    'onViewUpdate'
                )
            } else {
                const $$views = $views?.map((v) => ({
                    ...v,
                    def: false,
                }))
                const $newView = {
                    ...$view,
                    def: true,
                    isShow: true,
                    id: genViewId('view'),
                }
                update({ views: [...$$views, $newView], currentView: $newView }, 'onViewUpdate')
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

const rawCurrentView: ConfigT = {
    filters: [],
    queries: [],
    ids: [],
    pinnedIds: [],
    selectedIds: [],
    sortBy: '',
    version: 0,
    updated: false,
    updateColumn: false,
    id: 'all',
    measuredWidths: {},
    resizeDeltas: {},
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
        //  direct set should not trigger changed event
        setCurrentView: (view) => {
            set(
                {
                    currentView: _.isEmpty(view) ? rawCurrentView : view,
                },
                false,
                'setCurrentView'
            )
        },
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
            const prev = get()
            // view id change will not tiggered changed status, current view only saved in local,
            set({ currentView: view }, false, 'onCurrentViewIdChange')
            // @ts-ignore
            store.getState().onCurrentViewChange?.(get(), prev)
        },
        onCurrentViewFiltersChange: (filters) => update({ filters }, 'onCurrentViewFiltersChange'),
        onCurrentViewQueriesChange: (queries) =>
            update({ queries: queries.filter((v) => !!v.value) }, 'onCurrentViewQueriesChange'),
        onCurrentViewColumnsChange: (selectedIds: any[], pinnedIds: any[], ids: any[]) =>
            update({ selectedIds, pinnedIds, ids, updateColumn: true }, 'onCurrentViewColumnsChange'),
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
        getMeasuredWidths: () => get().currentView.measuredWidths,
        setMeasuredWidths: (measuredWidths: Record<string, any>) =>
            set(
                {
                    currentView: {
                        ...get().currentView,
                        measuredWidths: { ...measuredWidths },
                    },
                },
                undefined,
                'setMeasuredWidths'
            ),
        getResizeDeltas: () => get().currentView.resizeDeltas,
        setResizeDeltas: (resizeDeltas: Record<string, any>) =>
            set(
                {
                    currentView: {
                        ...get().currentView,
                        resizeDeltas: { ...resizeDeltas },
                    },
                },
                undefined,
                'setResizeDeltas'
            ),
        onColumnResize: (key: string, delta = 0) => {
            const { resizeDeltas = {} } = get().currentView
            const prev = resizeDeltas?.[key] ?? 0
            update(
                {
                    resizeDeltas: { ...resizeDeltas, [key]: Math.max(prev + delta, 0) },
                },
                'onColumnResize'
            )
        },
    }
}

// eslint-disable-next-line @typescript-eslint/no-unused-vars
const createViewInteractiveSlice: IStateCreator<IViewInteractiveState> = (set, get, store) => ({
    viewEditing: {},
    viewModelShow: false,
    onShowViewModel: (viewModelShow, viewEditing: any) =>
        set(
            {
                viewEditing,
                viewModelShow,
            },
            undefined,
            'onShowViewModel'
        ),
})

// eslint-disable-next-line @typescript-eslint/no-unused-vars
const createTableStateInitSlice: IStateCreator<ITableStateInitState> = (set, get, store) => ({
    isInit: false,
    key: 'table',
    initStore: (obj?: Record<string, any>) => {
        set(
            {
                ...(!_.isEmpty(obj) ? _.pick(obj, Object.keys(rawInitialState)) : rawInitialState),
                isInit: true,
            },
            undefined,
            'initStore'
        )
    },
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
    rowSelectedRecords: Array<any>
    onSelectMany: (args: any[], records: any[]) => void
    onSelectNone: () => void
    onSelectOne: (id: any, record: any) => void
    onNoSelect: (id: any) => void
    setRowSelectedIds: (ids: any[]) => void
}

// eslint-disable-next-line @typescript-eslint/no-unused-vars
const createRowSlice: IStateCreator<IRowState> = (set, get, store) => {
    const update = (updateAttrs: Partial<IRowState>, name?: string) => {
        // FIXME missing types
        // @ts-ignore
        const { onRowSelectedChange } = store.getState()

        set(updateAttrs, undefined, name)
        onRowSelectedChange?.(updateAttrs.rowSelectedIds)
    }

    return {
        rowSelectedIds: [],
        rowSelectedRecords: [],
        onSelectMany: (incomingRows: any[], records: any[]) =>
            update(
                {
                    rowSelectedIds: [...incomingRows],
                    rowSelectedRecords: [...records],
                },
                'onSelectMany'
            ),
        onSelectNone: () =>
            update(
                {
                    rowSelectedIds: [],
                    rowSelectedRecords: [],
                },
                'onSelectNone'
            ),
        onNoSelect: (id: any) => {
            // FIXME missing types
            // @ts-ignore
            const { getId } = store.getState()
            const selectedRecords = [...get().rowSelectedRecords]
            const selectedRowIds = new Set(get().rowSelectedIds)
            selectedRowIds.delete(id)
            const index = selectedRecords.findIndex((r) => getId?.(r) === id)
            if (index > -1) {
                selectedRecords.splice(index, 1)
            }

            update(
                {
                    rowSelectedIds: Array.from(selectedRowIds),
                    rowSelectedRecords: selectedRecords,
                },
                'onNoSelect'
            )
        },
        onSelectOne: (id: any, record: any) => {
            // FIXME missing types
            // @ts-ignore
            const { getId } = store.getState()

            const selectedRecords = [...get().rowSelectedRecords]
            const selectedRowIds = new Set(get().rowSelectedIds)
            if (selectedRowIds.has(id)) {
                selectedRowIds.delete(id)
            } else {
                selectedRowIds.add(id)
            }
            const index = selectedRecords.findIndex((r) => getId?.(r) === id)
            if (index > -1) {
                selectedRecords.splice(index, 1)
            } else {
                selectedRecords.push(record)
            }

            update(
                {
                    rowSelectedIds: Array.from(selectedRowIds),
                    rowSelectedRecords: selectedRecords,
                },
                'onSelectOne'
            )
        },
        setRowSelectedIds: (rowSelectedIds: any[]) => {
            // FIXME missing types
            // @ts-ignore
            const { getId } = store.getState()
            const selectedRecords = [...get().rowSelectedRecords]

            update(
                {
                    rowSelectedIds,
                    rowSelectedRecords: selectedRecords.filter((r) => rowSelectedIds.includes(getId(r))),
                },
                'setRowSelectedIds'
            )
        },
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

const getColumnsIds = (state: ITableState) => {
    const { currentView: view, columns } = state
    const { pinnedIds = [], ids = [] }: ConfigT = view
    const columnIds = columns?.map((c) => c.key) ?? []
    if (!view.id || (view.id === 'all' && !view.updateColumn)) {
        return Array.from(new Set([...pinnedIds, ...columnIds]))
    }
    return ids
}

const getColumns = (state: ITableState) => {
    const { currentView: view, columns } = state
    if (!columns) return []
    if (!view) return columns
    const { pinnedIds = [] }: ConfigT = view
    const columnsMap = _.keyBy(columns, (c) => c.key)
    const ids = getColumnsIds(state)
    return ids
        .filter((id: any) => id in columnsMap)
        .map((id: any) => {
            const _pin = columnsMap[id].pin ?? undefined
            return {
                ...columnsMap[id],
                pin: pinnedIds.includes(id) ? 'LEFT' : _pin,
            }
        })
}

// eslint-disable-next-line @typescript-eslint/no-unused-vars
const createComputeSlice: IStateCreator<{
    compute: IComputeState
}> = (set, get, store) => {
    const update = (updateAttrs: Partial<IComputeState>, name?: string) => {
        const curr = get().compute
        set(
            {
                compute: {
                    ...curr,
                    ...updateAttrs,
                },
            },
            undefined,
            name
        )
    }

    return {
        compute: { columns: [], rows: [], sortIndex: -1, sortDirection: undefined },
        computeColumns: () => {
            console.log('computeColumns')
            return update({ columns: getColumns(get()) }, 'setColumns')
        },
        computeSortIndex: () => {
            console.log('computeSortIndex')
            const { sortBy } = get().currentView || {}
            const sortIndex = get().columns?.findIndex((c) => c.key === sortBy)
            return update({ sortIndex }, 'computeSortIndex')
        },
        computeRows: () => {
            const { getId, records } = store.getState()
            console.log('computeRows')
            const rows =
                records?.map((raw, index) => {
                    // console.log(raw, getId)
                    return {
                        id: getId?.(raw) ?? index.toFixed(),
                        data: raw,
                    }
                }) ?? []

            return update({ rows }, 'computeRows')
        },
    }
}

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
        // @ts-ignore
        ...createComputeSlice(...a),
        ...initState,
        key: name,
    })

    if (isPersist) {
        return create<ITableState>()(
            subscribeWithSelector(
                devtools(
                    persist(actions as any, {
                        name,
                        partialize: (state) =>
                            Object.fromEntries(
                                Object.entries(state).filter(([_key]) => !Object.keys(rawInitialState).includes(_key))
                            ),
                    })
                )
            )
        )
    }

    const useStore = create<ITableState>()(
        subscribeWithSelector(
            devtools(actions, {
                serialize: {
                    options: {
                        undefined: true,
                        // function: function (fn) {
                        //     // return fn.toString()
                        //     return 'function'
                        // },
                        // @
                        function: undefined,
                        symbol: undefined,
                    },
                },
                stateSanitizer: (state) => {
                    return {
                        ...state,
                        rows: '<<LONG_BLOB>>',
                        columns: '<<LONG_BLOB>>',
                        wrapperRef: '<<DOM>>',
                        compute: '<<compute>>',
                    }
                },
            })
        )
    )
    return useStore as UseBoundStore<StoreApi<ITableState>>
}

export type IStore = ReturnType<typeof createCustomStore>

export default createCustomStore

export const useEvaluationStore = createCustomStore('evaluations', {}, false)
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
export const useDatasetStore = createCustomStore(
    'dataset',
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

export const useFineTuneEvaluationStore = createCustomStore('ft-evaluations', {}, false)
export const useFineTuneEvaluationDetailStore = createCustomStore(
    'ft-evaluation-detail',
    {
        compare: {
            comparePinnedKey: '',
            compareShowCellChanges: true,
            compareShowDiffOnly: false,
        },
    },
    false
)

export { stateSelector, currentQueriesSelector, currentViewSelector }
