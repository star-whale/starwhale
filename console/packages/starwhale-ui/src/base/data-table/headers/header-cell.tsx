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
import shallow from 'zustand/shallow'
import useGridSort from '@starwhale/ui/GridTable/hooks/useGridSort'

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
    sortable: s.sortable,
    rowSelectedIds: s.rowSelectedIds,
})

const HeaderCell = React.forwardRef<HTMLDivElement, HeaderCellPropsT>((props, ref) => {
    //@ts-ignore
    const locale: { datatable: DataTableLocaleT } = React.useContext(LocaleContext)
    const [css, theme] = themedUseStyletron()
    const [focusVisible, setFocusVisible] = React.useState(false)
    const checkboxRef = React.useRef(null)
    const { sortable, rowSelectedIds, queryinline, columnleinline } = useStore(selector, shallow)

    const { sortIndex, sortDirection } = useGridSort()

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
        () =>
            [
                {
                    label: props.isPin ? locale.datatable.columnUnPinColumn : locale.datatable.columnPinColumn,
                    type: 'pin',
                },
                sortable && { label: locale.datatable.columnSortAsc, type: 'sortAsc' },
                sortable && { label: locale.datatable.columnSortDesc, type: 'sortDesc' },
            ].filter(Boolean),
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
                    minWidth: props.index == 0 ? '120px' : 'auto',
                })
            )}
            title={props.title}
            onMouseEnter={props.onMouseEnter as any}
            onMouseLeave={props.onMouseLeave as any}
            onFocus={handleFocus}
            onBlur={handleBlur}
        >
            {props.index === 0 && !props.isSelectable && (queryinline || columnleinline) && <p className='w-38px' />}
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
                {rowSelectedIds && rowSelectedIds.length > 0 && props.index === 0 && `(${rowSelectedIds.length})`}
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
                    alignItems: 'center',
                    flex: 0,
                })}
            >
                {(props.isHovered || props.sortDirection) &&
                    !props.compareable &&
                    sortable &&
                    sortIndex === props.index && (
                        <div
                            style={{
                                // display: 'flex',
                                alignItems: 'center',
                                // position: 'absolute',
                                display: 'block',
                                right: 15,
                            }}
                        >
                            {props.sortDirection === SORT_DIRECTIONS.DESC && <IconFont type='a-sortdesc' />}
                            {(props.sortDirection === SORT_DIRECTIONS.ASC || !props.sortDirection) && (
                                <IconFont type='a-sortasc' />
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
