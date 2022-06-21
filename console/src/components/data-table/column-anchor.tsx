/*
Copyright (c) Uber Technologies, Inc.

This source code is licensed under the MIT license found in the
LICENSE file in the root directory of this source tree.
*/

import React from 'react'

import { StyledLink } from 'baseui/link'
import { useStyletron } from 'baseui'

import Column from './column'
import { COLUMNS } from './constants'
import type { ColumnT, RenderCellT, RenderFilterT, SharedColumnOptionsT } from './types'

type ValueT = { content: string; href: string }

type ReplacementElementAs = React.Component<{
    href: string
    children: string
}>

type OptionsT = {
    elementAs?: ReplacementElementAs | string
} & SharedColumnOptionsT<ValueT>

type FilterParametersT = {}

type AnchorColumnT = ColumnT<ValueT, FilterParametersT>

function AnchorFilter(props: any): React.ReactElement {
    return <div>not implemented for anchor column </div>
}

function AnchorCell(props: any) {
    const [css] = useStyletron()
    return (
        <div
            className={css({
                display: '-webkit-box',
                WebkitLineClamp: 1,
                WebkitBoxOrient: 'vertical',
                overflow: 'hidden',
            })}
        >
            <StyledLink $as={props.elementAs} href={props.value.href}>
                {props.value.content}
            </StyledLink>
        </div>
    )
}

function AnchorColumn(options: OptionsT): AnchorColumnT {
    return Column({
        kind: COLUMNS.ANCHOR,
        buildFilter: function (params: any) {
            return function (data: any) {
                return true
            }
        },
        cellBlockAlign: options.cellBlockAlign,
        fillWidth: options.fillWidth,
        filterable: false,
        mapDataToValue: options.mapDataToValue,
        maxWidth: options.maxWidth,
        minWidth: options.minWidth,
        // @ts-ignore
        renderCell: function RenderAnchorCell(props) {
            return <AnchorCell {...props} elementAs={options.elementAs} />
        },
        // @ts-ignore
        renderFilter: AnchorFilter,
        sortable: options.sortable === undefined ? true : options.sortable,
        sortFn: function (a: any, b: any) {
            return a.content.localeCompare(b.content)
        },
        title: options.title,
        key: options.key,
        pin: options.pin,
    })
}

export default AnchorColumn
