import { RecordListVo } from '@starwhale/core'
import { Types } from '../base/data-table'
import { IStore, ITableState } from '../base/data-table/store'
import { RowT } from '../base/data-table/types'

export interface ITableProps extends IToolBarProps, IPaginationProps {
    records: RecordListVo['records']
    columnTypes: RecordListVo['columnTypes']
    batchActions?: Types.BatchActionT[]
    rowActions?: Types.RowActionT[]
    paginationProps?: IPaginationProps
    onSave?: (props: any) => void
    onChange?: (state: ITableState, prevState: ITableState) => void
    onColumnSave?: (props: any) => void
    onSelectionChange?: (rows: RowT[]) => void
    filterable?: boolean
    searchable?: boolean
    compareable?: boolean
    isLoading?: boolean
    queryable?: boolean
    selectable?: boolean
    queryinline?: boolean
    id?: string
    data: any[]
    columns: any[]
    storeRef?: React.MutableRefObject<ITableState | undefined>
    emptyMessage?: React.ReactNode
    emptyColumnMessage?: React.ReactNode
    title?: React.ReactNode
    titleOfCompare?: React.ReactNode
    children?: React.ReactNode
}

export interface IToolBarProps {
    columnable?: boolean
    viewable?: boolean
}

export interface IPaginationProps {
    total?: number
    start?: number
    count?: number
    onPageChange?: (page: number) => void
    afterPageChange?: (page: number) => void
}

export type IContextGridTable = {
    store?: IStore
    storeKey?: string
    initState?: Partial<ITableState>
} & ITableProps
