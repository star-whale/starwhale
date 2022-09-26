import * as React from 'react'

import { Checkbox } from 'baseui/checkbox'
import { useStyletron } from 'baseui'
import { ChevronDown, ChevronUp } from 'baseui/icon'
import { isFocusVisible } from '@/utils/focusVisible'
import { StatefulPopover, PLACEMENT } from 'baseui/popover'
import { StatefulMenu } from 'baseui/menu'
import IconFont from '@/components/IconFont'
import cn from 'classnames'
import { SortDirectionsT } from './types'
import { SORT_DIRECTIONS } from './constants'
import Button from '../Button'
import { LocaleContext } from './locales'

type HeaderCellPropsT = {
    index: number
    isHovered: boolean
    isMeasured?: boolean
    isSelectable: boolean
    isSelectedAll: boolean
    isSelectedIndeterminate: boolean
    onMouseEnter: (num: number) => void
    onMouseLeave: (num: number) => void
    onSelectAll: () => void
    onSelectNone: () => void
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
}

const HeaderCell = React.forwardRef<HTMLDivElement, HeaderCellPropsT>((props, ref) => {
    const locale = React.useContext(LocaleContext)
    const [css, theme] = useStyletron()
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
        [props.onPin, props.onSort, props.index]
    )

    return (
        <div
            data-type='header-cell'
            ref={ref}
            role='button'
            tabIndex={0}
            className={cn(
                props.isHovered ? 'header-cell--hovered' : undefined,
                props.isFocus ? 'header-cell--focused' : undefined,
                css({
                    ...theme.typography.font350,
                    alignItems: 'center',
                    // backgroundColor,
                    boxSizing: 'border-box',
                    color: theme.colors.contentPrimary,
                    cursor: props.sortable ? 'pointer' : undefined,
                    display: props.isMeasured ? 'inline-flex' : 'flex',
                    flexGrow: 1,
                    height: '100%',
                    flexWrap: 'nowrap',
                    whiteSpace: 'nowrap',
                    outline: focusVisible ? `3px solid ${theme.colors.accent}` : 'none',
                    outlineOffset: '-3px',
                    backgroundColor: 'var(--color-brandTableHeaderBackground)',
                    fontWeight: 'bold',
                    borderBottomWidth: 0,
                    fontSize: '14px',
                    lineHeight: '16px',
                    paddingTop: '15px',
                    paddingBottom: '15px',
                    paddingLeft: props.index === 0 ? '20px' : '12px',
                    paddingRight: '12px',
                    borderRight: props.isFocus ? '1px dashed #2B65D9' : undefined,
                    borderLeft: props.isFocus ? '1px dashed #2B65D9' : undefined,
                    borderTop: props.isFocus ? '1px dashed #2B65D9' : undefined,
                })
            )}
            title={props.title}
            // @ts-ignore
            onMouseEnter={props.onMouseEnter}
            // @ts-ignore
            onMouseLeave={props.onMouseLeave}
            onKeyUp={(event) => {
                if (event.key === 'Enter' && props.sortable) {
                    props.onSort(
                        props.index,
                        props.sortDirection === SORT_DIRECTIONS.ASC ? SORT_DIRECTIONS.DESC : SORT_DIRECTIONS.ASC
                    )
                }
            }}
            // onClick={(event) => {
            // Avoid column sort if select-all checkbox click.
            // @ts-ignore
            // if (checkboxRef.current && checkboxRef.current.contains(event.target)) {
            //     return
            // }
            // }}
            onFocus={handleFocus}
            onBlur={handleBlur}
        >
            {props.isSelectable && (
                <span className={css({ paddingRight: theme.sizing.scale300 })} ref={checkboxRef}>
                    <Checkbox
                        onChange={() => {
                            if (props.isSelectedAll || props.isSelectedIndeterminate) {
                                props.onSelectNone()
                            } else {
                                props.onSelectAll()
                            }
                        }}
                        checked={props.isSelectedAll || props.isSelectedIndeterminate}
                        isIndeterminate={props.isSelectedIndeterminate}
                    />
                </span>
            )}
            {props.title}

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
                    flex: 1,
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
                                                <div style={{ display: 'flex', justifyContent: 'space-between' }}>
                                                    {item.label} {icon?.[item.type as keyof typeof icon]}
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
                        }}
                    >
                        <IconFont
                            type='more'
                            style={{
                                display: props.isHovered && !props.compareable ? 'block' : 'none',
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
