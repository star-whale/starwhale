import { RecordListVo } from '@starwhale/core'
import { RowT, BatchActionT, RowActionT } from '../base/data-table/types'
import { IStore, ITableState } from './store/store'

export type IGridState = ITableState & IGridExtenalState

export interface ITableProps extends IToolBarProps, IPaginationProps {
    records?: RecordListVo['records']
    columnTypes?: RecordListVo['columnTypes']
    rows?: RowT[]
    batchActions?: BatchActionT[]
    rowActions?: RowActionT[]
    paginationProps?: IPaginationProps
    rowSelectedIds?: string[]
    filterable?: boolean
    searchable?: boolean
    compareable?: boolean
    isLoading?: boolean
    queryable?: boolean
    sortable?: boolean
    selectable?: boolean
    previewable?: boolean
    queryinline?: boolean
    columnleinline?: boolean
    fillable?: boolean
    removable?: boolean
    id?: string
    columns?: any[]
    storeRef?: React.MutableRefObject<ITableState | undefined>
    emptyMessage?: React.ReactNode
    emptyColumnMessage?: React.ReactNode
    title?: React.ReactNode | string
    titleOfCompare?: React.ReactNode | string
    children?: React.ReactNode
    resizableColumnWidths?: boolean
    rowHighlightIndex?: number
    rowHeight?: number
    headlineHeight?: number
    onSave?: (props: any) => void
    onChange?: (state: ITableState, prevState: ITableState) => void
    onColumnSave?: (props: any) => void
    onColumnsChange?: (props: any) => void
    onViewsChange?: (state: any, nextState: any) => void
    currentView?: any
    onCurrentViewChange?: (state: any, nextState: any) => void
    onSelectionChange?: (rows: RowT[]) => void
    onRowHighlightChange?: (index: number) => void
    onIncludedRowsChange?: (rows: RowT[]) => void
    onRowSelectedChange?: (rows: RowT[]) => void
    onRemove?: (id: string) => void
    getId?: (record: any) => string | any
    // @FIXME type
    onInit?: (state: Partial<IGridState>) => void
    getColumns?: () => any[]
    paginationable?: boolean
    wrapperRef?: React.MutableRefObject<HTMLElement | undefined>
}

export type IGridExtenalState = Pick<
    ITableProps,
    | 'sortable'
    | 'fillable'
    | 'queryable'
    | 'selectable'
    | 'removable'
    | 'queryinline'
    | 'columnleinline'
    | 'getId'
    | 'onSave'
    | 'onRemove'
    | 'onPageChange'
    | 'onViewsChange'
    | 'onColumnsChange'
    | 'onCurrentViewChange'
    | 'onIncludedRowsChange'
    | 'onRowHighlightChange'
    | 'onRowSelectedChange'
    | 'page'
    | 'records'
    | 'columnTypes'
    | 'rowSelectedIds'
    | 'currentView'
    // inline set
    | 'getColumns'
    | 'wrapperRef'
    | 'rows'
>

export interface IToolBarProps {
    columnable?: boolean
    viewable?: boolean
}

export interface IPaginationProps {
    page?: {
        pageNum: number
        pageSize: number
        [key: string]: any
    }
    onPageChange?: (page: any) => void
    afterPageChange?: (page: any) => void
}

export type IContextGridTable = {
    store?: IStore
    storeKey?: string
    initState?: Partial<ITableState>
} & ITableProps
