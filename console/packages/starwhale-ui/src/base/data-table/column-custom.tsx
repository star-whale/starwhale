/* eslint-disable */

import { FilterT } from '@starwhale/ui/Search'
import Column from './column'
import { COLUMNS } from './constants'
import type { ColumnT, RenderCellT, RenderFilterT, SharedColumnOptionsT } from './types'

// I could not re-use the ColumnT type to build this.. tried to spread the ColumnT
// and define renderFilter, etc. to optional, but required status was maintained.
type OptionsT<ValueT, FilterParamsT> = {
    // @ts-ignore
    renderCell: RenderCellT<ValueT>
    renderFilter?: RenderFilterT<ValueT, FilterParamsT>
    buildFilter?: (args: FilterParamsT) => (args: ValueT) => boolean
    textQueryFilter?: (text: string, args: ValueT) => boolean
    sortFn?: (valueA: ValueT, valueB: ValueT) => number
    getFilters?: () => FilterT
    buildFilters?: (builder, args: FilterT) => FilterT
} & SharedColumnOptionsT<ValueT>

function CustomColumn<ValueT, FilterParamsT>(options: OptionsT<ValueT, FilterParamsT>): ColumnT<ValueT, FilterParamsT> {
    // @ts-ignore
    return Column({ kind: COLUMNS.CUSTOM, ...options })
}

export default CustomColumn
