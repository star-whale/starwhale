/* eslint-disable */

import * as React from 'react'

import { useStyletron } from 'baseui'

import { CategoricalFilter } from './column-categorical'
import Column from './column'
import { COLUMNS } from './constants'
import { ColumnT, FilterTypes, SharedColumnOptionsT } from './types'
import { LocaleContext } from 'baseui/locale'

type OptionsT = SharedColumnOptionsT<boolean>

type FilterParametersT = {
    selection: Set<boolean>
    description: string
    exclude: boolean
}

type BooleanColumnT = ColumnT<boolean, FilterParametersT>

function mapSelection<X, Y>(selection: Set<X>, transform: (x: X) => Y): Set<Y> {
    const coercedSelection = new Set<Y>()
    selection.forEach((item) => coercedSelection.add(transform(item)))
    return coercedSelection
}

function BooleanFilter(props: any) {
    const locale: any = React.useContext(LocaleContext)

    let selectionString = new Set()
    if (props.filterParams && props.filterParams.selection) {
        selectionString = mapSelection(props.filterParams.selection, (i) =>
            i ? locale.datatable.booleanFilterTrue : locale.datatable.booleanFilterFalse
        )
    }

    return (
        <CategoricalFilter
            close={props.close}
            data={[locale.datatable.booleanFilterTrue, locale.datatable.booleanFilterFalse]}
            // @ts-ignore
            filterParams={
                props.filterParams
                    ? {
                          selection: selectionString,
                          description: props.filterParams.description,
                          exclude: props.filterParams.exclude,
                      }
                    : undefined
            }
            setFilter={(params) => {
                props.setFilter({
                    selection: mapSelection(params.selection, (i) => i === locale.datatable.booleanFilterTrue),
                    exclude: params.exclude,
                    description: params.description,
                })
            }}
        />
    )
}

function BooleanCell(props: any) {
    const [css, theme] = useStyletron()
    const locale: any = React.useContext(LocaleContext)
    return (
        <div
            className={css({
                textAlign: props.value ? 'right' : 'left',
                minWidth: theme.sizing.scale1400,
                width: '100%',
            })}
        >
            {props.value ? locale.datatable.booleanColumnTrueShort : locale.datatable.booleanColumnFalseShort}
        </div>
    )
}

function BooleanColumn(options: OptionsT): BooleanColumnT {
    return Column({
        kind: COLUMNS.BOOLEAN,
        buildFilter: function (params: any) {
            return function (data: any) {
                const included = params.selection.has(data)
                return params.exclude ? !included : included
            }
        },
        cellBlockAlign: options.cellBlockAlign,
        fillWidth: options.fillWidth,
        filterable: options.filterable === undefined ? true : options.filterable,
        mapDataToValue: options.mapDataToValue,
        maxWidth: options.maxWidth,
        minWidth: options.minWidth,
        // @ts-ignore
        renderCell: BooleanCell,
        // @ts-ignore
        renderFilter: BooleanFilter,
        sortable: options.sortable === undefined ? true : options.sortable,
        sortFn: function (a, b) {
            if (a === b) return 0
            return a ? -1 : 1
        },
        title: options.title,
        key: options.key,
        pin: options.pin,
        filterType: options.filterType ?? FilterTypes.enum,
        columnType: options.columnType,
    })
}

export default BooleanColumn
