import * as React from 'react'

import { Checkbox } from 'baseui/checkbox'
import { useStyletron } from 'baseui'
import { ChevronDown, ChevronUp } from 'baseui/icon'
import { isFocusVisible } from '@/utils/focusVisible'

import IconFont from '@/components/IconFont'
import cn from 'classnames'
import type { SortDirectionsT } from './types'
import { SORT_DIRECTIONS } from './constants'
import Button from '../Button'

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
    onFocus?: (arg: boolean) => void
    onSort: (num: number) => void
    sortable: boolean
    sortDirection: SortDirectionsT
    title: string
    compareable?: boolean
}

const HeaderCell = React.forwardRef<HTMLDivElement, HeaderCellPropsT>((props, ref) => {
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

    // const backgroundColor = props?.isHovered ? theme.colors.backgroundSecondary : theme.colors.backgroundPrimary

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
                    fontSize: 14,
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
                if (event.key === 'Enter') {
                    props.onSort(props.index)
                }
            }}
            onClick={(event) => {
                // Avoid column sort if select-all checkbox click.
                // @ts-ignore
                if (checkboxRef.current && checkboxRef.current.contains(event.target)) {
                    return
                }
                if (props.sortable) {
                    props.onSort(props.index)
                }
            }}
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
                    alignItems: 'center',
                })}
            >
                {(props.isHovered || props.sortDirection) && props.sortable && (
                    <div
                        style={{
                            display: 'flex',
                            alignItems: 'center',
                            position: 'absolute',
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
            </div>
        </div>
    )
})
HeaderCell.displayName = 'HeaderCell'
HeaderCell.defaultProps = {
    isMeasured: false,
    compareable: false,
    isFocus: false,
    onNoSelect: () => {},
    onFocus: () => {},
}

export default HeaderCell
