import React, { useMemo, useRef, useCallback } from 'react'
import { SHAPE, SIZE, KIND } from 'baseui/button'
import Button from '@starwhale/ui/Button'
import { Popover, PLACEMENT } from 'baseui/popover'
import { useStyletron } from 'baseui'
import { Tag } from 'baseui/tag'
import { expandBorder } from '@/utils'
import { useEventCallback } from '@starwhale/core/utils'
import FilterOperateSelector, { FilterOperateSelectorValueT } from './filter-operate-selector'
import type { ColumnT } from './types'
import { LocaleContext } from 'baseui/locale'
import FilterShell from './filter-shell'
import Checkbox from '../../Checkbox'
import IconFont from '../../IconFont'
import useTranslation from '@/hooks/useTranslation'

// type CategoricalColumnT = ColumnT<string, FilterParametersT>

type CategoricalFilterProps = {
    // eslint-disable-next-line react/no-unused-prop-types
    isInline?: boolean
    // eslint-disable-next-line react/no-unused-prop-types
    columns: ColumnT[]
    // eslint-disable-next-line react/no-unused-prop-types
    rows?: any[]
    // eslint-disable-next-line react/no-unused-prop-types
    close?: () => void
    // eslint-disable-next-line react/no-unused-prop-types
    setFilter?: (args: FilterOperateSelectorValueT[]) => void
    filters?: FilterOperateSelectorValueT[]
}

export const CategoricalFilter = React.forwardRef<
    { getCategories: () => FilterOperateSelectorValueT[] },
    CategoricalFilterProps
>((props, ref) => {
    const [css, theme] = useStyletron()
    const [t] = useTranslation()
    // const locale = React.useContext(LocaleContext)

    const [categories, setCategories] = React.useState<FilterOperateSelectorValueT[]>(
        [...(props.filters ?? [])] ?? [{ disable: false }]
    )

    const checkboxHeaderStyles = css({
        marginBottom: '24px',
        display: 'flex',
        alignItems: 'center',
        gap: '8px',
    })
    const checkboxStyles = css({
        marginBottom: '12px',
        display: 'flex',
        alignItems: 'center',
        gap: '8px',
        position: 'relative',
    })

    const selectedCategories = React.useMemo(() => {
        return categories.filter((category) => !category.disable)
    }, [categories])

    const handleSelectAll = useCallback(
        (e) => {
            const $categories = categories.map((v) => ({
                ...v,
                disable: !e.target?.checked,
            }))
            setCategories([...$categories])
        },
        [categories, setCategories]
    )

    const handleSelectOne = useCallback(
        (index) => {
            const $categories = categories.map((v) => ({
                ...v,
            }))
            $categories[index].disable = !categories[index].disable
            setCategories($categories)
        },
        [categories, setCategories]
    )

    const handleAddFilter = useEventCallback(() => {
        const $categories = categories.map((v) => ({
            ...v,
        }))
        $categories.push({
            disable: false,
        })
        setCategories($categories)
    })

    const handleDeleteOne = useCallback(
        (index) => {
            const $categories = categories.map((v) => ({
                ...v,
            }))
            $categories.splice(index, 1)
            setCategories($categories)
        },
        [categories, setCategories]
    )

    const handleChange = useCallback(
        (v: FilterOperateSelectorValueT, index: number) => {
            const $categories = categories.map((c) => ({
                ...c,
            }))
            $categories[index] = v
            setCategories($categories)
        },
        [categories]
    )

    React.useImperativeHandle(ref, () => ({ getCategories: () => categories }), [categories])

    return (
        <div>
            <div className={checkboxHeaderStyles}>
                <Checkbox
                    checked={selectedCategories.length > 0 && selectedCategories.length === categories.length}
                    onChange={handleSelectAll}
                />
                Filter ({selectedCategories.length})
            </div>

            <div
                className={css({
                    maxHeight: '500px',
                    overflowY: 'auto',
                    marginTop: theme.sizing.scale600,
                })}
            >
                {Boolean(categories.length) &&
                    categories.map((category, i) => (
                        <div className={checkboxStyles} key={i}>
                            <Checkbox checked={!category.disable} onChange={() => handleSelectOne(i)} />
                            <FilterOperateSelector
                                // @ts-ignore
                                columns={props.columns}
                                // id={i}
                                value={category}
                                onChange={(v) => handleChange(v, i)}
                            />
                            <Button as='link' type='button' onClick={() => handleDeleteOne(i)}>
                                <IconFont type='item-reduce' size={16} style={{ color: 'rgba(2,16,43,0.40)' }} />
                            </Button>
                        </div>
                    ))}
            </div>

            <Button
                as='link'
                type='button'
                className={css({
                    alignSelf: 'flex-start',
                    marginTop: '4px',
                    marginLeft: '26px',
                    color: ' rgba(2,16,43,0.60)',
                    display: 'flex',
                    cursor: 'pointer',
                    alignItems: 'center',
                })}
                onClick={handleAddFilter}
                startEnhancer={() => <IconFont type='item-add' size={16} />}
            >
                {t('table.filter.add')}
            </Button>
        </div>
    )
})

CategoricalFilter.defaultProps = {
    isInline: false,
    rows: [],
    filters: [],
    setFilter: () => {},
    close: () => {},
}

type PropsT = {
    columns: ColumnT[]
    filters: any[]
    rows: any[]
    onFilterSet?: (filterParams: any[]) => void
    onSave?: (filterParams: any[]) => void
    onSaveAs?: (filterParams: any[]) => void
}

function FilterOperateMenu(props: PropsT) {
    // const [, theme] = useStyletron()
    const locale = React.useContext(LocaleContext)
    const [isOpen, setIsOpen] = React.useState(false)
    const [, setQuery] = React.useState('')
    const filters = useMemo(() => [...props.filters] ?? [], [props.filters])

    const handleClose = React.useCallback(() => {
        setIsOpen(false)
        setQuery('')
    }, [])

    const columns = React.useMemo(() => {
        return props.columns
    }, [props.columns])

    const handleClick = React.useCallback(() => {
        if (isOpen) {
            handleClose()
        } else {
            setIsOpen(true)
        }
    }, [isOpen, handleClose])
    const controlRef = useRef(null)

    const cb = useCallback(() => {}, [])
    const handeApply = useCallback(() => {
        if (controlRef.current) {
            const categories = (controlRef.current as any).getCategories()
            props.onFilterSet?.(categories)
            handleClose()
        }
    }, [controlRef, handleClose, props])
    const handelSave = useCallback(() => {
        if (controlRef.current) {
            const categories = (controlRef.current as any).getCategories()
            props.onSave?.(categories)
            handleClose()
        }
    }, [controlRef, handleClose, props])
    const handelSaveAs = useCallback(() => {
        if (controlRef.current) {
            const categories = (controlRef.current as any).getCategories()
            props.onSaveAs?.(categories)
            handleClose()
        }
    }, [controlRef, handleClose, props])

    const handleSelectNone = useCallback(
        (e) => {
            e.preventDefault()
            props.onFilterSet?.([])
        },
        [props]
    )

    const enableFitlers = useMemo(() => {
        return Array.from(filters.filter((v) => !v.disable))
    }, [filters])

    return (
        <Popover
            focusLock
            returnFocus
            placement={PLACEMENT.bottomLeft}
            content={() => {
                // @ts-ignore
                return (
                    <FilterShell
                        exclude={false}
                        onExcludeChange={cb}
                        onApply={handeApply}
                        onSave={handelSave}
                        onSaveAs={handelSaveAs}
                    >
                        <CategoricalFilter
                            ref={controlRef}
                            rows={props.rows}
                            columns={columns}
                            setFilter={props.onFilterSet}
                            filters={props.filters}
                        />
                    </FilterShell>
                )
            }}
            onClick={handleClick}
            onClickOutside={handleClose}
            onEsc={handleClose}
            isOpen={isOpen}
            ignoreBoundary
        >
            {/* fix: popover wrong postion with raw baseui/button */}
            <div>
                <Button
                    shape={SHAPE.default}
                    size={SIZE.compact}
                    kind={KIND.tertiary}
                    overrides={{
                        BaseButton: {
                            style: {
                                ...expandBorder('1px', 'solid', '#CFD7E6'),
                                backgroundColor: 'transparent',
                                paddingTop: '6px',
                                paddingBottom: '6px',
                                lineHeight: '20px',
                            },
                        },
                    }}
                    startEnhancer={() => <IconFont type='filter' />}
                    endEnhancer={() =>
                        enableFitlers.length > 0 ? (
                            <Tag
                                color='#EEF1F6'
                                size='small'
                                overrides={{
                                    Root: {
                                        style: {
                                            marginTop: '0px',
                                            marginBottom: '0px',
                                            marginLeft: '0px',
                                            marginRight: '0px',
                                            height: '20px',
                                            backgroundColor: ' #EEF1F6',
                                            color: '#2B65D9',
                                            ...expandBorder('0px'),
                                        },
                                    },
                                }}
                                onActionClick={handleSelectNone}
                            >
                                {enableFitlers.length}
                            </Tag>
                        ) : null
                    }
                >
                    {locale.datatable.filterAdd}
                </Button>
            </div>
        </Popover>
    )
}

export default FilterOperateMenu

// op
