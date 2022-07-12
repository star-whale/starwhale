/*
Copyright (c) Uber Technologies, Inc.

This source code is licensed under the MIT license found in the
LICENSE file in the root directory of this source tree.
*/
// @flow

import * as React from 'react'

import { COLUMNS, SORT_DIRECTIONS } from './constants'

export type SortDirectionsT = typeof SORT_DIRECTIONS.ASC | typeof SORT_DIRECTIONS.DESC | null

export type ColumnsT =
    | typeof COLUMNS.ANCHOR
    | typeof COLUMNS.BOOLEAN
    | typeof COLUMNS.CATEGORICAL
    | typeof COLUMNS.CUSTOM
    | typeof COLUMNS.DATETIME
    | typeof COLUMNS.NUMERICAL
    | typeof COLUMNS.STRING

export enum FilterTypes {
    sysDefault = 'sysDefault',
    default = 'default',
    string = 'string',
    number = 'number',
    enum = 'enum',
    // float = 'float',
    // boolean = 'boolean',
    // date = 'date',
}

// These options are available on all column kinds. Most have additional
// unique options depending on the data visualization requirements.
export type SharedColumnOptionsT<ValueT> = {
    cellBlockAlign?: 'start' | 'center' | 'end'
    fillWidth?: boolean
    filterable?: boolean
    // flowlint-next-line unclear-type:off
    mapDataToValue: (data: any) => ValueT
    maxWidth?: number
    minWidth?: number
    sortable?: boolean
    title: string
    key?: string
    pin?: 'LEFT'
    filterType?: keyof typeof FilterTypes
    onAsyncChange?: (value: ValueT, columnIndex: number, rowIndex: number) => Promise<void>
}

export type RenderCellT<ValueT> = React.Component<{
    value: ValueT
    isMeasured?: boolean
    isSelected?: boolean
    onSelect?: () => void
    textQuery?: string
    x: number
    y: number
}>

export type RenderFilterT<ValueT, FilterParamsT> = React.Component<{
    close: () => void
    data: ValueT[]
    filterParams?: FilterParamsT
    setFilter: (args: FilterParamsT) => void
}>

// flowlint-next-line unclear-type:off
export type ColumnT<ValueT = any, FilterParamsT = any> = {
    kind: ColumnsT
    sortable: boolean
    renderCell: RenderCellT<ValueT>
    renderFilter: RenderFilterT<ValueT, FilterParamsT>
    buildFilter: (args: FilterParamsT) => (args: ValueT) => boolean
    textQueryFilter?: (text: string, value: ValueT) => boolean
    sortFn: (valueA: ValueT, valueB: ValueT) => number
} & SharedColumnOptionsT<ValueT>

export type RowT = {
    id: number | string
    // flowlint-next-line unclear-type:off
    data: any
}

export type BatchActionT = {
    label: string
    onClick: (args: {
        clearSelection: () => any
        event: React.SyntheticEvent<HTMLButtonElement>
        selection: RowT[]
    }) => any
    renderIcon?: React.Component<{ size: number }>
}

export type RowActionT = {
    label: string
    onClick: (args: { event: React.SyntheticEvent<HTMLButtonElement>; row: RowT }) => any
    renderIcon: React.Component<{ size: number }>
    renderButton?: React.Component<any>
}

type ImperativeMethodsT = {
    getRows: () => RowT[]
}
export type ControlRefT = {
    current: ImperativeMethodsT | null
}

export type ConfigT = {
    id?: string
    def?: boolean
    isShow?: boolean
    selectedIds?: Array<any>
    sortedIds?: Array<any>
    pinnedIds?: Array<any>
    filters?: Array<any>
    name?: string
}

export type StatefulDataTablePropsT = {
    batchActions?: BatchActionT[]
    columns: ColumnT[]
    rawColumns: ColumnT[]
    emptyMessage?: string | React.Component<any>
    filterable?: boolean
    initialFilters?: any[]
    initialSelectedRowIds?: Set<number | string>
    initialSortIndex?: number
    initialSortDirection?: SortDirectionsT
    loading?: boolean
    loadingMessage?: string | React.Component<any>
    onFilterSet?: (v: any[]) => void
    onFilterAdd?: (v: string, { description: string }: any) => any
    onFilterRemove?: (v: string) => any
    onIncludedRowsChange?: (rows: RowT[]) => void
    onRowHighlightChange?: (rowIndex: number, row: RowT) => void
    onSelectionChange?: (rows: RowT[]) => any
    resizableColumnWidths?: boolean
    rows: RowT[]
    rowActions?: RowActionT[] | ((row: RowT) => RowActionT[])
    rowHeight?: number
    rowHighlightIndex?: number
    searchable?: boolean
    columnable?: boolean
    viewable?: boolean
    controlRef?: ControlRefT
}

export type DataTablePropsT = {
    emptyMessage?: string | React.Component<any>
    filters?: any[]
    loading?: boolean
    loadingMessage?: string | React.Component<any>
    onIncludedRowsChange?: (rows: RowT[]) => void
    onRowHighlightChange?: (rowIndex: number, row: RowT) => void
    onSelectMany?: (rows: RowT[]) => void
    onSelectNone?: () => void
    onSelectOne?: (row: RowT) => void
    onSort?: (columnIndex: number) => void
    resizableColumnWidths?: boolean
    rowHighlightIndex?: number
    selectedRowIds?: Set<string | number>
    sortIndex?: number
    sortDirection?: SortDirectionsT
    textQuery?: string
} & StatefulDataTablePropsT

export type StatefulContainerPropsT = {
    children: {
        filters: any[]
        onFilterSet: (filterParams: any[]) => void
        onFilterAdd: (title: string, filterParams: any[]) => void
        onFilterRemove: (title: string) => void
        onIncludedRowsChange: (rows: RowT[]) => void
        onRowHighlightChange: (rowIndex: number, row: RowT) => void
        onSelectMany: (rows: RowT[]) => void
        onSelectNone: () => void
        onSelectOne: (row: RowT) => void
        onSort: (columnIndex: number) => void
        onTextQueryChange: (query: string) => void
        resizableColumnWidths: boolean
        rowHighlightIndex?: number
        selectedRowIds: Set<string | number>
        sortIndex: number
        sortDirection: SortDirectionsT
        textQuery: string
    }
} & StatefulDataTablePropsT
