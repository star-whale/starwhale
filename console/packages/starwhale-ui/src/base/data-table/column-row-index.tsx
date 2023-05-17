/* eslint-disable */

// @flow

import * as React from 'react'

import { useStyletron } from 'baseui'

import Column from './column'
import { COLUMNS } from './constants'
import type { ColumnT } from './types'

type ValueT = null
type FilterParametersT = {}

type RowIndexColumnT = ColumnT<ValueT, FilterParametersT>

function RowIndexFilter() {
    return <div>not implemented for row index column</div>
}
// @ts-ignore
function RowIndexCell(props) {
    const [css, theme] = useStyletron()
    return (
        <div
            className={css({
                display: 'flex',
                justifyContent: theme.direction !== 'rtl' ? 'flex-end' : 'flex-start',
                width: '100%',
            })}
        >
            {props.y + 1}
        </div>
    )
}

function RowIndexColumn(): RowIndexColumnT {
    return Column({
        // @ts-ignore
        kind: COLUMNS.ROW_INDEX,
        buildFilter: () => () => true,
        cellBlockAlign: 'start', // how to configure?
        fillWidth: false,
        filterable: false,
        mapDataToValue: () => null,
        // @ts-ignore
        renderCell: RowIndexCell,
        // @ts-ignore
        renderFilter: RowIndexFilter,
        sortable: false,
        sortFn: () => 0,
        title: '',
        key: 'row-index',
    })
}

export default RowIndexColumn
