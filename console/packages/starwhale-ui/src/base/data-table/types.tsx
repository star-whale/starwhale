import * as React from 'react'
import { COLUMNS, SORT_DIRECTIONS } from './constants'
import { FilterT } from '@starwhale/ui/Search'

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
    key: string
    pin?: 'LEFT'
    filterType?: keyof typeof FilterTypes
    onAsyncChange?: (value: ValueT, columnIndex: number, rowIndex: number) => Promise<void>
    renderCell?: RenderCellT<ValueT>
    columnType?: { name: string; type: string }
}

export type RenderCellT<ValueT> = React.FC<{
    data: any
    value: ValueT
    isMeasured?: boolean
    isSelected?: boolean
    removable?: boolean
    onSelect?: () => void
    textQuery?: string
    x: number
    y: number
    columnKey: string
}>

export type RenderFilterT<ValueT, FilterParamsT> = React.FC<{
    // close: () => void
    filterParams?: FilterParamsT
    // data: ValueT[]
    // setFilter: (args: FilterParamsT) => void
}>

export type ColumnT<ValueT = any, FilterParamsT = any> = {
    kind: ColumnsT
    sortable: boolean
    renderCell: RenderCellT<ValueT>
    renderFilter: RenderFilterT<ValueT, FilterParamsT>
    buildFilter: (args: FilterParamsT) => (args: ValueT) => boolean
    textQueryFilter?: (text: string, value: ValueT) => boolean
    sortFn: (valueA: ValueT, valueB: ValueT) => number
    getFilters: () => FilterT
    buildFilters: (builder, args?: FilterT) => FilterT
} & SharedColumnOptionsT<ValueT>

export type RowT = {
    id: number | string
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

export type QueryT = {
    property?: string
    op?: string
    value?: any
}

export type ConfigT = {
    updated?: boolean
    updateColumn?: boolean
    version?: number
    id?: string
    def?: boolean
    isShow?: boolean
    selectedIds?: Array<any>
    ids?: Array<any>
    pinnedIds?: Array<any>
    filters?: Array<any>
    queries?: QueryT[]
    name?: string
    sortBy?: string
    sortDirection?: SortDirectionsT
    measuredWidths?: Record<string, number>
    resizeDeltas?: Record<string, number>
}

export type StatefulDataTablePropsT = {
    columns: ColumnT[]
    rawColumns?: ColumnT[]
    emptyMessage?: React.ReactNode
    emptyColumnMessage?: React.ReactNode
    filterable?: boolean
    initialFilters?: any[]
    initialSelectedRowIds?: Set<number | string>
    initialSortIndex?: number
    initialSortDirection?: SortDirectionsT
    loading?: boolean
    loadingMessage?: React.ReactNode
    onFilterSet?: (v: any[]) => void
    onFilterAdd?: (v: string, { description }: any) => any
    onFilterRemove?: (v: string) => any
    onIncludedRowsChange?: (rows: RowT[]) => void
    onRowHighlightChange?: (rowIndex: number, row: RowT) => void
    onSelectionChange?: (rows: RowT[]) => any
    onSave?: (view: ConfigT) => void
    getId?: (row: RowT) => string | number
    resizableColumnWidths?: boolean
    rows: RowT[]
    rowActions?: RowActionT[] | ((row: RowT) => RowActionT[])
    rowHeight?: number
    rowHighlightIndex?: number
    searchable?: boolean
    columnable?: boolean
    viewable?: boolean
    compareable?: boolean
    removable?: boolean
    queryable?: boolean
    selectable?: boolean
    queryinline?: boolean
    previewable?: boolean
    controlRef?: ControlRefT
}

export type DataTablePropsT = {
    emptyMessage?: React.ReactNode
    emptyColumnMessage?: React.ReactNode
    filters?: any[]
    loading?: boolean
    loadingMessage?: React.ReactNode
    onIncludedRowsChange?: (rows: RowT[]) => void
    onRowHighlightChange?: (rowIndex: number, row: RowT) => void
    onSelectMany?: () => void
    onSelectNone?: () => void
    onSelectOne?: (row: RowT) => void
    onSort?: (columnIndex: number) => void
    resizableColumnWidths?: boolean
    rowHighlightIndex?: number
    selectedRowIds?: Set<string | number>
    sortIndex?: number
    sortDirection?: SortDirectionsT
    textQuery?: string
    getId?: (row: RowT) => string | number
    isRowSelected?: (row: RowT) => boolean
    isSelectedAll?: boolean
    isSelectedIndeterminate?: boolean
    onPreview?: (data: { record?: any; columnKey?: string }) => void
    onRemove?: (id?: string) => void
} & StatefulDataTablePropsT

export type StatefulContainerPropsT = {
    children: {
        onIncludedRowsChange: (rows: RowT[]) => void
        onRowHighlightChange: (rowIndex: number, row: RowT) => void
        onTextQueryChange: (query: string) => void
        resizableColumnWidths: boolean
        rowHighlightIndex?: number
        selectedRowIds: Set<string | number>
        textQuery: string
    }
} & StatefulDataTablePropsT
