import React, { useCallback, useEffect, useMemo, useReducer, useRef } from 'react'
import { Column, SortOrder } from '@/components/BaseTable'
import { Checkbox, LABEL_PLACEMENT } from 'baseui/checkbox'
import _ from 'lodash'

type TSortBy = { key: React.Key; order: string }

export interface ITableGridState {
    data: any[]
    columns: any[]
    tableConfigMap: {
        sortBy?: TSortBy
        enableMultiSelection?: boolean
    }
    dataConfigMap: Record<
        string,
        | {
              selected?: boolean
          }
        | any
    >
}

const initialState: ITableGridState = {
    data: [] as any[],
    columns: [] as any[],
    tableConfigMap: {
        enableMultiSelection: false,
    },
    dataConfigMap: {},
}

type ReducerAction<TType, TAdditional> = { type: TType } & TAdditional

export const createTableGridReducer =
    () =>
    (
        state: ITableGridState,
        action:
            | ReducerAction<'sort', { sortBy: TSortBy }>
            | ReducerAction<'selectRow', { rowData: any; value: boolean }>
            | ReducerAction<'resetData', { data: any[] }>
    ): ITableGridState => {
        // console.log('reducer', action)
        switch (action.type) {
            case 'sort': {
                const { sortBy } = action
                const order = sortBy.order === SortOrder.ASC ? 1 : -1
                const sortData = [...state.data]
                sortData.sort((a, b) => (a[sortBy.key] > b[sortBy.key] ? order : -order))

                return {
                    ...state,
                    tableConfigMap: {
                        ...state.tableConfigMap,
                        sortBy,
                    },
                    data: [...sortData],
                }
            }
            case 'selectRow': {
                const { rowData, value } = action
                const { id } = rowData
                const dataConfigMap = { ...state.dataConfigMap }

                if (!dataConfigMap[id]) dataConfigMap[id] = { selected: false }
                dataConfigMap[id].selected = value

                return {
                    ...state,
                    dataConfigMap,
                }
            }
            case 'resetData': {
                const { data } = action
                return {
                    ...state,
                    data,
                }
            }
            default:
                throw new Error('missing support')
        }
    }

export const useTableGridState = (props: ITableGridState) => {
    const reducerFn = useRef(createTableGridReducer()).current
    const [gridState, dispatch] = useReducer(reducerFn, {
        data: props.data,
        columns: props.columns,
        tableConfigMap: {
            ...initialState.tableConfigMap,
            ...props.tableConfigMap,
        },
        dataConfigMap: {
            ...initialState.dataConfigMap,
            ...props.dataConfigMap,
        },
    })

    // triggered only when data changed used by aysnc fetch
    useEffect(() => {
        dispatch({ type: 'resetData', data: props.data })
    }, [props.data])

    const { data, tableConfigMap, dataConfigMap } = gridState

    const handleColumnSort = useCallback(
        (payload: { sortBy: TSortBy }) => {
            dispatch({ type: 'sort', ...payload })
        },
        [dispatch]
    )

    const handleRowSelect = useCallback(
        (payload: { rowData: any; value: boolean }) => {
            dispatch({ type: 'selectRow', ...payload })
        },
        [dispatch]
    )

    const multiColumn = useMemo(
        () => ({
            key: 'multiselect',
            width: 50,
            align: Column.Alignment.CENTER,
            frozen: Column.FrozenDirection.LEFT,
            // headerRenderer: (props: any) => console.log(props),
            cellRenderer: ({ rowData, column }: any) => {
                return (
                    <Checkbox
                        checked={_.get(column?.dataConfigMap, [rowData.id, 'selected'])}
                        onChange={(e: React.ChangeEvent<HTMLInputElement>) => {
                            handleRowSelect({ value: e.target.checked, rowData })
                        }}
                        labelPlacement={LABEL_PLACEMENT.right}
                    />
                )
            },
        }),
        [handleRowSelect]
    )

    const $columns = useMemo(() => {
        let computeColumns = []
        const { enableMultiSelection } = gridState.tableConfigMap
        if (enableMultiSelection) computeColumns.push(multiColumn)
        computeColumns.push(...gridState.columns)
        computeColumns = computeColumns.map((col) => ({
            ...col,
            dataConfigMap: gridState.dataConfigMap,
        }))
        return computeColumns
    }, [multiColumn, gridState])

    return {
        gridState,
        data,
        tableConfigMap,
        dataConfigMap,
        columns: $columns,
        handleColumnSort,
        handleRowSelect,
    }
}
