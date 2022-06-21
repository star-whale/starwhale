/*
Copyright (c) Uber Technologies, Inc.

This source code is licensed under the MIT license found in the
LICENSE file in the root directory of this source tree.
*/

import Column from './column'
import { COLUMNS } from './constants'
import type { ColumnT, RenderCellT, RenderFilterT, SharedColumnOptionsT } from './types'

// I could not re-use the ColumnT type to build this.. tried to spread the ColumnT
// and define renderFilter, etc. to optional, but required status was maintained.
type OptionsT<ValueT, FilterParamsT> = {
    renderCell: RenderCellT<ValueT>
    renderFilter?: RenderFilterT<ValueT, FilterParamsT>
    buildFilter?: (args: FilterParamsT) => (args: ValueT) => boolean
    textQueryFilter?: (text: string, args: ValueT) => boolean
    sortFn?: (valueA: ValueT, valueB: ValueT) => number
} & SharedColumnOptionsT<ValueT>

function CustomColumn<ValueT, FilterParamsT>(options: OptionsT<ValueT, FilterParamsT>): ColumnT<ValueT, FilterParamsT> {
    // @ts-ignore
    return Column({ kind: COLUMNS.CUSTOM, ...options })
}

export default CustomColumn
