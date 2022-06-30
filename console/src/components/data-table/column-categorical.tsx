/* eslint-disable */
/*
Copyright (c) Uber Technologies, Inc.

This source code is licensed under the MIT license found in the
LICENSE file in the root directory of this source tree.
*/

import * as React from 'react'

import { Button, SIZE, KIND } from 'baseui/button'
import { ButtonGroup } from 'baseui/button-group'
import { Checkbox, StyledLabel } from 'baseui/checkbox'
import { Search } from 'baseui/icon'
import { Input, SIZE as INPUT_SIZE } from 'baseui/input'
import { useStyletron, withStyle } from 'baseui'
import { LabelSmall } from 'baseui/typography'

import Column from './column'
import { COLUMNS } from './constants'
import type { ColumnT, SharedColumnOptionsT } from './types'
import { LocaleContext } from './locales'
import FilterShell from './filter-shell'
import { matchesQuery, splitByQuery, HighlightCellText } from './text-search'

type OptionsT = SharedColumnOptionsT<string>

type FilterParametersT = {
    description: string
    exclude: boolean
    selection: Set<string>
}

type CategoricalColumnT = ColumnT<string, FilterParametersT>

function InputBefore() {
    const [css, theme] = useStyletron()
    return (
        <div
            className={css({
                display: 'flex',
                alignItems: 'center',
                paddingLeft: theme.sizing.scale500,
            })}
        >
            <Search size='18px' />
        </div>
    )
}

function FilterQuickControls(props: { onSelectAll: () => void; onClearSelection: () => void }) {
    const locale: any = React.useContext(LocaleContext)

    return (
        <ButtonGroup size={SIZE.mini} kind={KIND.tertiary}>
            <Button type='button' onClick={props.onSelectAll}>
                {locale.datatable.categoricalFilterSelectAll}
            </Button>
            <Button type='button' onClick={props.onClearSelection}>
                {locale.datatable.categoricalFilterSelectClear}
            </Button>
        </ButtonGroup>
    )
}

// @ts-ignore
const StyledHighlightLabel = withStyle(StyledLabel, (props: any) => {
    let style = {
        whiteSpace: 'pre',
        color: props.$isActive ? props.$theme.colors.contentPrimary : props.$theme.colors.contentSecondary,
    }

    // TODO
    if (!props.$isFirst) {
        // @ts-ignore
        style.paddingLeft = 0
    }

    return style
})

function HighlightCheckboxLabel(props: any) {
    const { children, ...restProps } = props

    if (!props.query) {
        return <StyledLabel {...restProps}>{children}</StyledLabel>
    }

    return splitByQuery(children, props.query).map((el, i) => {
        if (matchesQuery(el, props.query)) {
            return (
                <StyledHighlightLabel {...restProps} key={i} $isFirst={!i} $isActive>
                    {el}
                </StyledHighlightLabel>
            )
        }
        return (
            <StyledHighlightLabel {...restProps} key={i} $isFirst={!i}>
                {el}
            </StyledHighlightLabel>
        )
    })
}

type CategoricalFilterProps = {
    data: string[]
    close: () => void
    setFilter: (args: FilterParametersT) => void
    filterParams?: FilterParametersT
}

export function CategoricalFilter(props: CategoricalFilterProps) {
    const [css, theme] = useStyletron()
    const locale = React.useContext(LocaleContext)
    const [selection, setSelection] = React.useState<Set<string>>(
        props.filterParams ? props.filterParams.selection : new Set()
    )
    const [exclude, setExclude] = React.useState(props.filterParams ? props.filterParams.exclude : false)
    const [query, setQuery] = React.useState('')
    const categories = React.useMemo(() => {
        return props.data.reduce((set, category) => set.add(category), new Set())
    }, [props.data])

    const checkboxStyles = css({ marginBottom: theme.sizing.scale200 })

    const showQuery = Boolean(categories.size >= 10)
    const filteredCategories = Array.from(categories, (c) => c).filter((c) => matchesQuery(c as string, query))

    return (
        <FilterShell
            exclude={exclude}
            onExcludeChange={() => setExclude(!exclude)}
            onApply={() => {
                props.setFilter({
                    description: Array.from(selection).join(', '),
                    exclude,
                    selection,
                })
                props.close()
            }}
        >
            {showQuery && (
                <Input
                    size={INPUT_SIZE.compact}
                    overrides={{ Before: InputBefore }}
                    value={query}
                    // @ts-ignore
                    onChange={(event) => setQuery(event.target?.value)}
                    clearable
                />
            )}

            {!query && (
                <div
                    style={{
                        marginTop: showQuery ? theme.sizing.scale600 : undefined,
                    }}
                >
                    <FilterQuickControls
                        onSelectAll={() => {
                            categories.forEach((c) => selection.add(c as string))
                            setSelection(new Set(selection))
                        }}
                        onClearSelection={() => {
                            setSelection(new Set())
                        }}
                    />
                </div>
            )}

            <div
                className={css({
                    maxHeight: '256px',
                    overflowY: 'auto',
                    marginTop: theme.sizing.scale600,
                })}
            >
                {!filteredCategories.length && <LabelSmall>{locale.datatable.categoricalFilterEmpty}</LabelSmall>}

                {Boolean(filteredCategories.length) &&
                    filteredCategories.map((category, i) => (
                        <div className={checkboxStyles} key={i}>
                            {/* @ts-ignore */}
                            <Checkbox
                                checked={selection.has(category as string)}
                                onChange={() => {
                                    if (selection.has(category as string)) {
                                        selection.delete(category as string)
                                    } else {
                                        selection.add(category as string)
                                    }
                                    setSelection(new Set(selection))
                                }}
                                overrides={{
                                    Label: { component: HighlightCheckboxLabel, props: { query } },
                                }}
                            >
                                {category}
                            </Checkbox>
                        </div>
                    ))}
            </div>
        </FilterShell>
    )
}

function CategoricalCell(props: any) {
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
            {props.textQuery ? <HighlightCellText text={props.value} query={props.textQuery} /> : props.value}
        </div>
    )
}

function CategoricalColumn(options: OptionsT): CategoricalColumnT {
    return Column({
        kind: COLUMNS.CATEGORICAL,
        buildFilter: function (params) {
            return function (data) {
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
        renderCell: CategoricalCell,
        // @ts-ignore
        renderFilter: CategoricalFilter,
        sortable: options.sortable === undefined ? true : options.sortable,
        sortFn: function (a, b) {
            return a.localeCompare(b)
        },
        textQueryFilter: function (textQuery, data) {
            return data.toLowerCase().includes(textQuery.toLowerCase())
        },
        title: options.title,
        key: options.key,
        pin: options.pin,
    })
}

export default CategoricalColumn
