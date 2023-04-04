/* eslint-disable */

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
import { LocaleContext } from 'baseui/locale'
import FilterShell from './filter-shell'
import { matchesQuery, splitByQuery } from './text-search'
import { Tag, KIND as TAG_KIND, VARIANT as TAG_VARIANT } from 'baseui/tag'
import { Popover, PLACEMENT } from 'baseui/popover'
import IconFont from '../../IconFont'

type OptionsT = SharedColumnOptionsT<string>

type FilterParametersT = {
    description: string
    exclude: boolean
    selection: Set<string>
}

type CategoricalColumnT = ColumnT<string, FilterParametersT>

const TAG_COLORS = [
    TAG_KIND.blue,
    TAG_KIND.green,
    TAG_KIND.red,
    TAG_KIND.yellow,
    TAG_KIND.orange,
    TAG_KIND.purple,
    TAG_KIND.brown,
]

function hasCode(v: string) {
    var hash = 0,
        i,
        chr
    if (v.length === 0) return hash
    for (i = 0; i < v.length; i++) {
        chr = v.charCodeAt(i)
        hash = (hash << 5) - hash + chr
        hash |= 0 // Convert to 32bit integer
    }
    return hash
}

function getColor(v: string) {
    return TAG_COLORS[hasCode(String(v)) % TAG_COLORS.length]
}

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
    const categories: Set<string> = React.useMemo(() => {
        return props.data.reduce((set, category) => {
            // multi category
            category?.split(',').forEach((c) => set.add(c))
            return set
        }, new Set<string>())
    }, [props.data])

    const checkboxStyles = css({
        'marginBottom': theme.sizing.scale200,
        ':hover': {
            backgroundColor: theme.colors.backgroundSecondary,
        },
    })

    const showQuery = Boolean(categories.size >= 10)
    const filteredCategories: string[] = Array.from(categories, (c) => c)
        .filter((c) => !!c)
        .filter((c) => matchesQuery(c as string, query))

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
                    filteredCategories.map((category: string, i) => (
                        <div className={checkboxStyles} key={i}>
                            {/* @ts-ignore */}
                            <Checkbox
                                checked={selection.has(category)}
                                onChange={() => {
                                    if (selection.has(category)) {
                                        selection.delete(category)
                                    } else {
                                        selection.add(category)
                                    }
                                    setSelection(new Set(selection))
                                }}
                                overrides={{
                                    Root: {
                                        style: {
                                            alignItems: 'center',
                                        },
                                    },
                                    // Label: { component: HighlightCheckboxLabel, props: { query } },
                                }}
                            >
                                <Tag
                                    key={i}
                                    kind={getColor(category)}
                                    size={'small'}
                                    closeable={false}
                                    variant={TAG_VARIANT.solid}
                                >
                                    {category as string}
                                </Tag>
                            </Checkbox>
                        </div>
                    ))}
            </div>
        </FilterShell>
    )
}

type CategoricalEditPopoverProps = {
    data: string[]
    selected?: string[]
    close: () => void
    setFilter: (args: FilterParametersT) => void
    filterParams?: FilterParametersT
}

export function CategoricalEditPopover(props: CategoricalEditPopoverProps) {
    const [css, theme] = useStyletron()
    const locale = React.useContext(LocaleContext)
    const inputRef = React.useRef(null)
    React.useEffect(() => {
        if (inputRef.current) {
            // @ts-ignore
            inputRef.current?.focus()
        }
    }, [inputRef.current])
    const [selection, setSelection] = React.useState<Set<string>>(
        props.filterParams ? props.filterParams.selection : new Set()
    )
    const [exclude, setExclude] = React.useState(props.filterParams ? props.filterParams.exclude : false)
    const [query, setQuery] = React.useState('')
    const categories = React.useMemo(() => {
        let set = new Set()
        selection.forEach((c) => set.add(c))
        return props.data.reduce((set, category) => {
            category?.split(',').forEach((c) => set.add(c))
            return set
        }, set)
    }, [props.data, selection, selection])

    const checkboxStyles = css({
        'marginBottom': theme.sizing.scale200,
        'cursor': 'pointer',
        ':hover': {
            backgroundColor: theme.colors.backgroundSecondary,
        },
    })
    const showQuery = true
    const filteredCategories = Array.from(categories, (c) => c)
        .filter((c) => !!c)
        .filter((c) => matchesQuery(c as string, query))

    return (
        <FilterShell
            hasExclude={false}
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
            <LabelSmall $style={{ marginBottom: '10px' }}>{locale.datatable.categoricalTagSelectOrCreate}</LabelSmall>

            {showQuery && (
                <Input
                    inputRef={inputRef}
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
                                    Root: {
                                        style: {
                                            alignItems: 'center',
                                        },
                                    },
                                }}
                            >
                                <Tag
                                    key={i}
                                    kind={getColor(category as string)}
                                    size={'small'}
                                    closeable={false}
                                    variant={TAG_VARIANT.solid}
                                >
                                    {category as string}
                                </Tag>
                            </Checkbox>
                        </div>
                    ))}

                {!filteredCategories.length && (
                    <div className={checkboxStyles}>
                        {query && (
                            <LabelSmall
                                onClick={() => {
                                    selection.add(query as string)
                                    setSelection(new Set(selection))
                                    setQuery('')
                                }}
                            >
                                Create:
                                <Tag
                                    key={99}
                                    kind={TAG_COLORS[0]}
                                    size={'small'}
                                    closeable={false}
                                    variant={TAG_VARIANT.solid}
                                >
                                    {query as string}
                                </Tag>
                            </LabelSmall>
                        )}
                    </div>
                )}
            </div>
        </FilterShell>
    )
}

interface ICategoricalCellProps {
    value?: string
    isHovered?: boolean
    onAsyncChange: (value: string) => void
}

function CategoricalCell(props: any) {
    const [css, theme] = useStyletron()
    const locale = React.useContext(LocaleContext)

    const tags = React.useMemo(() => {
        let set = new Set<string>()
        props.value
            ?.split(',')
            .filter((c: string) => !!c)
            .forEach((c: string) => set.add(c))
        return Array.from(set)
    }, [props.value])

    const [isOpen, setIsOpen] = React.useState(false)
    const handleClose = React.useCallback(() => {
        setIsOpen(false)
    }, [])

    console.log('CategoricalCell', props, tags)

    return (
        <div>
            <Popover
                focusLock
                returnFocus
                placement={PLACEMENT.bottomLeft}
                content={() => {
                    return (
                        <CategoricalEditPopover
                            close={handleClose}
                            setFilter={(args) => {
                                props?.onAsyncChange(args.description)
                                setIsOpen(false)
                            }}
                            data={tags}
                        />
                    )
                }}
                onClick={() => {
                    if (isOpen) {
                        handleClose()
                    } else {
                        setIsOpen(true)
                    }
                }}
                onClickOutside={handleClose}
                onEsc={handleClose}
                isOpen={isOpen}
                ignoreBoundary
            >
                <div
                    className={css({
                        'flex': 1,
                        'display': 'flex',
                        'justifyContent': 'space-between',
                        'cursor': 'pointer',
                        ':hover': {
                            backgroundColor: theme.colors.backgroundSecondary,
                        },
                    })}
                >
                    <div
                        className={css({
                            display: '-webkit-box',
                            WebkitLineClamp: 1,
                            WebkitBoxOrient: 'vertical',
                            overflow: 'hidden',
                            flex: 1,
                        })}
                    >
                        {tags.map((tag: string, i: number) => (
                            <Tag key={i} kind={getColor(tag)} closeable={false} variant={TAG_VARIANT.solid}>
                                {tag}
                            </Tag>
                        )) ?? ' '}
                        {/* {props.textQuery ? <HighlightCellText text={props.value} query={props.textQuery} /> : props.value} */}
                    </div>

                    {props.isHoverd && (
                        <IconFont
                            size={25}
                            type='more'
                            style={{
                                position: 'absolute',
                                cursor: 'pointer',
                                right: 0,
                                justifySelf: 'right',
                                alignSelf: 'center',
                            }}
                        />
                    )}
                </div>
            </Popover>
        </div>
    )
}

function CategoricalTagsColumn(options: OptionsT): CategoricalColumnT {
    return Column({
        kind: COLUMNS.CATEGORICAL,
        buildFilter: function (params) {
            return function (data) {
                // console.log('buildFilter', data, params)
                const included = Array.from(params.selection).every((c) => {
                    return data.includes(c)
                })
                // const included = params.selection.has(data)
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
            return a?.localeCompare(b)
        },
        textQueryFilter: function (textQuery, data) {
            return data.toLowerCase().includes(textQuery.toLowerCase())
        },
        title: options.title,
        onAsyncChange: options.onAsyncChange,
        key: options.key,
        pin: options.pin,
    })
}

export default CategoricalTagsColumn
