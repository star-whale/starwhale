import * as React from 'react'

import { useStyletron } from 'baseui'
import { ChevronDown, ChevronUp } from 'baseui/icon'
import { isFocusVisible } from '@/utils/focusVisible'
import { StatefulPopover, PLACEMENT, Popover, TRIGGER_TYPE } from 'baseui/popover'
import { StatefulMenu } from 'baseui/menu'
import IconFont from '../../../IconFont'
import cn from 'classnames'
import { SortDirectionsT } from '../types'
import { SORT_DIRECTIONS } from '../constants'
import Button from '../../../Button'
import { LocaleContext } from 'baseui/locale'
import Checkbox from '../../../Checkbox'
import { themedUseStyletron } from '../../../theme/styletron'
import { DataTableLocaleT } from '../locale'
import { IGridState } from '@starwhale/ui/GridTable/types'
import { useStore } from '@starwhale/ui/GridTable/hooks/useStore'
import useGridQuery from '@starwhale/ui/GridTable/hooks/useGridQuery'
import useGrid from '@starwhale/ui/GridTable/hooks/useGrid'
import ConfigColumns from '@starwhale/ui/GridTable/components/ConfigColumns'
import { useHover, useHoverDirty } from 'react-use'

type HeaderCellPropsT = {
    index: number
    wrapperWidth?: number
    isHovered: boolean
    isMeasured?: boolean
    isSelectable: boolean
    isSelectedAll?: boolean
    isQueryInline?: boolean
    isSelectedIndeterminate?: boolean
    selectedRowIds: Set<any>
    onMouseEnter: (num: number) => void
    onMouseLeave: (num: number) => void
    onSelectAll?: () => void
    onSelectNone?: () => void
    onNoSelect?: (id: any) => void
    isFocus?: boolean
    isPin?: boolean
    onFocus?: (arg: boolean) => void
    onSort: (num: number, direction: SortDirectionsT) => void
    onPin?: (num: number, bool: boolean) => void
    sortable: boolean
    sortDirection: SortDirectionsT
    title: string
    compareable?: boolean
    removable?: boolean
    querySlot?: React.ReactNode
}

const selector = (s: IGridState) => ({
    queryinline: s.queryinline,
    columnleinline: s.columnleinline,
    onCurrentViewColumnsChange: s.onCurrentViewColumnsChange,
    wrapperRef: s.wrapperRef,
})

const HeaderCell = React.forwardRef<HTMLDivElement, HeaderCellPropsT>((props, ref) => {
    //@ts-ignore
    const locale: { datatable: DataTableLocaleT } = React.useContext(LocaleContext)
    const [css, theme] = themedUseStyletron()
    const [focusVisible, setFocusVisible] = React.useState(false)
    const checkboxRef = React.useRef(null)

    const handleFocus = (event: React.SyntheticEvent) => {
        if (isFocusVisible(event as any)) {
            setFocusVisible(true)
        }
    }

    const handleBlur = () => {
        if (focusVisible !== false) {
            setFocusVisible(false)
        }
    }

    const COLUMN_OPTIONS = React.useMemo(
        () => [
            { label: props.isPin ? locale.datatable.columnUnPinColumn : locale.datatable.columnPinColumn, type: 'pin' },
            { label: locale.datatable.columnSortAsc, type: 'sortAsc' },
            { label: locale.datatable.columnSortDesc, type: 'sortDesc' },
        ],
        [props.isPin, locale]
    )

    const handleColumnOptionSelect = React.useCallback(
        (option: any) => {
            if (option.type === 'pin') {
                props.onPin?.(props.index, !props.isPin)
            } else if (option.type === 'sortAsc') {
                props.onSort(props.index, SORT_DIRECTIONS.ASC)
            } else if (option.type === 'sortDesc') {
                props.onSort(props.index, SORT_DIRECTIONS.DESC)
            }
        },
        [props]
    )
    return (
        <div
            data-type='header-cell'
            ref={ref}
            role='button'
            tabIndex={0}
            className={cn(
                'header-cell',
                props.isHovered ? 'header-cell--hovered' : undefined,
                props.isFocus ? 'header-cell--focused' : undefined,
                css({
                    ...theme.typography.font350,
                    alignItems: 'center',
                    boxSizing: 'border-box',
                    color: theme.colors.contentPrimary,
                    cursor: props.sortable ? 'pointer' : undefined,
                    display: props.isMeasured ? 'inline-flex' : 'flex',
                    flexGrow: 1,
                    height: '100%',
                    flexWrap: 'nowrap',
                    whiteSpace: 'nowrap',
                    // outline: focusVisible ? `3px solid ${theme.colors.accent}` : 'none',
                    outlineOffset: '-3px',
                    backgroundColor: props.isHovered
                        ? theme.brandTableHeaderBackgroundHover
                        : theme.brandTableHeaderBackground,
                    fontWeight: 'bold',
                    borderBottomWidth: 0,
                    fontSize: '14px',
                    lineHeight: '26px',
                    paddingTop: '10px',
                    paddingBottom: '10px',
                    paddingLeft: '12px',
                    paddingRight: '12px',
                    borderRight: props.isFocus ? `1px dashed ${theme.brandPrimary}` : undefined,
                    borderLeft: props.isFocus ? `1px dashed ${theme.brandPrimary}` : undefined,
                    borderTop: props.isFocus ? `1px dashed ${theme.brandPrimary}` : undefined,
                    maxWidth: '100%',
                })
            )}
            title={props.title}
            onMouseEnter={props.onMouseEnter as any}
            onMouseLeave={props.onMouseLeave as any}
            onFocus={handleFocus}
            onBlur={handleBlur}
        >
            {props.index === 0 && <HeaderFirstMenu {...props} />}
            {props.isSelectable && (
                <span className={css({ paddingRight: theme.sizing.scale300 })} ref={checkboxRef}>
                    <Checkbox
                        onChange={() => {
                            if (props.isSelectedAll || props.isSelectedIndeterminate) {
                                props.onSelectNone?.()
                            } else {
                                props.onSelectAll?.()
                            }
                        }}
                        checked={props.isSelectedAll || props.isSelectedIndeterminate}
                        isIndeterminate={props.isSelectedIndeterminate}
                    />
                </span>
            )}
            <span
                className={css({
                    textOverflow: 'ellipsis',
                    overflow: 'hidden',
                    whiteSpace: 'nowrap',
                    display: 'block',
                    flex: 1,
                })}
            >
                {props.title}{' '}
                {props.selectedRowIds &&
                    props.selectedRowIds.size > 0 &&
                    props.index === 0 &&
                    `(${props.selectedRowIds.size})`}
            </span>
            {props.compareable && ((props.isHovered && props.index !== 0) || props.isFocus) && (
                <Button
                    onClick={() => {
                        props.onFocus?.(!props.isFocus)
                    }}
                    overrides={{
                        BaseButton: {
                            style: {
                                marginLeft: '10px',
                                display: 'flex',
                                alignItems: 'center',
                            },
                        },
                    }}
                    as='link'
                >
                    <IconFont type='pin' />
                </Button>
            )}
            <div
                className={css({
                    position: 'relative',
                    width: '100%',
                    display: 'flex',
                    alignItems: 'flex-end',
                    flex: 0,
                })}
            >
                {(props.isHovered || props.sortDirection) && props.sortable && (
                    <div
                        style={{
                            // display: 'flex',
                            alignItems: 'center',
                            position: 'absolute',
                            display: 'none',
                            right: -3,
                        }}
                    >
                        {props.sortDirection === SORT_DIRECTIONS.DESC && (
                            <ChevronDown
                                color={
                                    props.sortDirection ? theme.colors.contentPrimary : theme.colors.contentSecondary
                                }
                            />
                        )}
                        {(props.sortDirection === SORT_DIRECTIONS.ASC || !props.sortDirection) && (
                            <ChevronUp
                                color={
                                    props.sortDirection ? theme.colors.contentPrimary : theme.colors.contentSecondary
                                }
                            />
                        )}
                    </div>
                )}
                {props.isHovered && props.compareable && props.index !== 0 && (
                    <Button
                        // @ts-ignore
                        onClick={props.onNoSelect}
                        overrides={{
                            BaseButton: {
                                style: {
                                    marginLeft: '10px',
                                    display: 'flex',
                                    alignItems: 'center',
                                    position: 'absolute',
                                    right: 0,
                                },
                            },
                        }}
                        as='link'
                    >
                        <IconFont type='close' />
                    </Button>
                )}
                <StatefulPopover
                    focusLock
                    triggerType={TRIGGER_TYPE.hover}
                    placement={PLACEMENT.bottom}
                    content={({ close }) => (
                        <StatefulMenu
                            items={COLUMN_OPTIONS}
                            onItemSelect={({ item }) => {
                                handleColumnOptionSelect(item)
                                close()
                            }}
                            overrides={{
                                List: { style: { height: '130px', width: '150px' } },
                                Option: {
                                    props: {
                                        getItemLabel: (item: { label: string; type: string }) => {
                                            const icon = {
                                                pin: <IconFont type='pin' />,
                                                sortAsc: <IconFont type='a-sortasc' />,
                                                sortDesc: <IconFont type='a-sortdesc' />,
                                            }

                                            return (
                                                <div className='flex gap-10px items-center'>
                                                    {icon?.[item.type as keyof typeof icon]}
                                                    {item.label}
                                                </div>
                                            )
                                        },
                                    },
                                },
                            }}
                        />
                    )}
                >
                    {/* usesd for popover postion ref  */}
                    <div
                        style={{
                            alignItems: 'center',
                            marginLeft: 'auto',
                            right: 0,
                            top: -6,
                            display: 'flex',
                            width: '20px',
                            justifyContent: 'center',
                        }}
                    >
                        <IconFont
                            type='more'
                            style={{
                                visibility: props.isHovered && !props.compareable ? 'visible' : 'hidden',
                            }}
                        />
                    </div>
                </StatefulPopover>
            </div>
        </div>
    )
})
HeaderCell.displayName = 'HeaderCell'
HeaderCell.defaultProps = {
    isMeasured: false,
    compareable: false,
    isFocus: false,
    isPin: false,
    onNoSelect: () => {},
    onFocus: () => {},
    onPin: () => {},
}

export default HeaderCell

function HeaderFirstMenu(props: HeaderCellPropsT) {
    const locale: { datatable: DataTableLocaleT } = React.useContext(LocaleContext)
    const { wrapperRef, queryinline, columnleinline, onCurrentViewColumnsChange } = useStore(selector)
    const [css, theme] = themedUseStyletron()
    const { renderConfigQueryInline } = useGrid()
    const [isShowQuery, setIsShowQuery] = React.useState(false)

    // <span className={css({ paddingRight: theme.sizing.scale300 })}>{props.querySlot}</span>

    const COLUMN_OPTIONS = React.useMemo(
        () =>
            [
                queryinline && {
                    label: locale.datatable.columnQuery,
                    type: 'query',
                },
                { label: locale.datatable.columnConfig, type: 'column' },
            ].filter(Boolean),
        [queryinline, locale]
    )

    const handleColumnOptionSelect = React.useCallback(
        (option: any) => {
            if (option.type === 'query') {
                setIsShowQuery(true)
            }
        },
        [props]
    )

    if (!columnleinline && !queryinline) {
        return null
    }

    return (
        <>
            <div>
                {renderConfigQueryInline({
                    width: props.wrapperWidth,
                    isOpen: isShowQuery,
                    setIsOpen: setIsShowQuery as any,
                    mountNode: wrapperRef?.current,
                })}
            </div>
            {/* <ConfigColumns view={currentView} columns={originalColumns} onColumnsChange={onCurrentViewColumnsChange} /> */}
            <StatefulPopover
                focusLock
                triggerType={TRIGGER_TYPE.hover}
                placement={PLACEMENT.bottom}
                content={({ close }) => (
                    <StatefulMenu
                        items={COLUMN_OPTIONS}
                        onItemSelect={({ item }) => {
                            handleColumnOptionSelect(item)
                            close()
                        }}
                        overrides={{
                            List: { style: { height: '130px', width: '150px' } },
                            Option: {
                                props: {
                                    getItemLabel: (item: { label: string; type: string }) => {
                                        const icon = {
                                            query: <IconFont type='filter' />,
                                            column: <IconFont type='setting' />,
                                        }

                                        return (
                                            <div className='flex gap-10px items-center'>
                                                {icon?.[item.type as keyof typeof icon]}
                                                {item.label}
                                            </div>
                                        )
                                    },
                                },
                            },
                        }}
                    />
                )}
            >
                {/* usesd for popover postion ref  */}
                <Button as='link'>
                    <div
                        style={{
                            alignItems: 'center',
                            marginLeft: 'auto',
                            right: 0,
                            top: -6,
                            display: 'flex',
                            width: '30px',
                            justifyContent: 'center',
                            marginRight: '8px',
                        }}
                    >
                        <IconFont type='more' />
                    </div>
                </Button>
            </StatefulPopover>
        </>
    )
}
