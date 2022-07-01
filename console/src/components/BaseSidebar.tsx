/* eslint-disable react/no-unused-prop-types */

import { Navigation } from 'baseui/side-navigation'
import _ from 'lodash'
import React, { useCallback, useContext, useMemo } from 'react'
import { useLocation, useHistory, Link } from 'react-router-dom'
import useSidebarWidth from '@/hooks/useSidebarWidth'
import { useStyletron } from 'baseui'
import type { IconBaseProps } from 'react-icons/lib'
import { SidebarContext } from '@/contexts/SidebarContext'
import { AiOutlineDoubleLeft, AiOutlineDoubleRight } from 'react-icons/ai'
import Text from '@/components/Text'
import { createUseStyles } from 'react-jss'
import Logo from '@/components/Header/Logo'
import { headerHeight, sidebarExpandedWidth, sidebarFoldedWidth } from '@/consts'

const useBaseSideBarStyles = createUseStyles({
    sidebarWrapper: {
        display: 'flex',
        flexShrink: 0,
        flexDirection: 'column',
        overflow: 'hidden',
        overflowY: 'auto',
        background: 'var(--color-brandBgNav)',
        transition: 'all 200ms cubic-bezier(0.7, 0.1, 0.33, 1) 0ms',
    },

    siderLogo: {
        height: headerHeight,
    },
    siderTitle: {
        height: '56px',
        backgroundColor: 'var(--color-brandBgNavTitle)',
        color: 'var(--color-brandBgNavFont)',
        display: 'flex',
        gap: 14,
        fontSize: '14px',
        placeItems: 'center',
        padding: '8px 15px 8px 15px',
        overflow: 'hidden',
        textDecoration: 'none',
        marginBottom: '7px',
    },
    siderNavLink: {
        display: 'flex',
        alignItems: 'center',
        fontSize: 14,
        lineHeight: '40px',
        height: 40,
        gap: 12,
        textOverflow: 'ellipsis',
        whiteSpace: 'nowrap',
        overflow: 'hidden',
    },
})

export interface IComposedSidebarProps {
    style?: React.CSSProperties
    navStyle?: React.CSSProperties
}

export interface INavItem {
    title: string
    icon?: React.ComponentType<IconBaseProps> | React.ReactNode
    path?: string
    children?: INavItem[]
    disabled?: boolean
    helpMessage?: React.ReactNode
    activePathPattern?: RegExp
    isActive?: () => boolean
}

export interface IBaseSideBarProps extends IComposedSidebarProps {
    titleLink?: string
    title?: string
    icon?: React.ReactNode
    navItems: INavItem[]
}

export default function BaseSidebar({ navItems, style, title, icon, titleLink }: IBaseSideBarProps) {
    // eslint-disable-next-line react-hooks/exhaustive-deps
    const width = useSidebarWidth()
    const ctx = useContext(SidebarContext)
    const [, theme] = useStyletron()
    const styles = useBaseSideBarStyles({ theme })

    const history = useHistory()
    const location = useLocation()

    const baseuiNavItems = useMemo(() => {
        return navItems.map((item) => {
            const { icon: Icon } = item
            return {
                title: (
                    <div className={styles.siderNavLink} style={{ paddingLeft: ctx.expanded ? 24 : 18 }}>
                        {Icon}
                        {ctx.expanded && <span>{item.title}</span>}
                    </div>
                ),
                itemId: item.path,
            }
        })
    }, [ctx.expanded, navItems, styles])

    const activeItemId = useMemo(() => {
        const items = baseuiNavItems.slice().reverse()
        let activeItem = items.find((item_) => {
            const item = navItems.find((item__) => item_.itemId === item__.path)
            if (!item) {
                return false
            }
            if (item.activePathPattern) {
                return item.activePathPattern.test(location.pathname)
            }
            if (item.isActive) {
                return item.isActive()
            }
            return false
        })
        if (!activeItem) {
            activeItem = items.find((item_) => _.startsWith(location.pathname, item_.itemId))
        }
        return activeItem?.itemId
    }, [baseuiNavItems, location.pathname, navItems])

    const handleExpandedClick = useCallback(() => {
        if (ctx.expanded) {
            ctx.setExpanded(false)
        } else {
            ctx.setExpanded(true)
        }
    }, [ctx])

    return (
        <div
            className={styles.sidebarWrapper}
            style={{
                flexBasis: width,
                width,
                ...style,
            }}
        >
            <Logo className={styles.siderLogo} expanded={ctx.expanded} />
            {title && icon && (
                <Link
                    className={styles.siderTitle}
                    style={{
                        paddingLeft: !ctx.expanded ? 28 : 15,
                    }}
                    to={titleLink ?? '/projects'}
                >
                    {icon}
                    {ctx.expanded && (
                        <Text
                            style={{
                                overflow: 'hidden',
                                textOverflow: 'ellipsis',
                                whiteSpace: 'nowrap',
                                color: 'var(--color-brandWhite)',
                            }}
                        >
                            {title}
                        </Text>
                    )}
                </Link>
            )}
            <Navigation
                overrides={{
                    Root: {
                        style: {
                            fontSize: '14px',
                            flexGrow: 1,
                            fontWeight: 700,
                        },
                    },
                    NavItemContainer: {
                        style: {
                            height: 40,
                            padding: '5px 10px',
                            boxSizing: 'border-box',
                            borderLeftWidth: '0',
                            backgroundImage: 'none',
                            backgroundColor: 'none',
                        },
                    },
                    NavItem: {
                        style: ({ $active }) => {
                            if ($active)
                                return {
                                    'paddingLeft': '0',
                                    'paddingRight': '0',
                                    'paddingTop': '0',
                                    'paddingBottom': '0',
                                    'backgroundImage': 'none',
                                    'borderLeftWidth': '0',
                                    'backgroundColor': 'var(--color-brandPrimary)',
                                    'borderRadius': '8px',
                                    'color': 'var(--color-brandBgNavFont)',
                                    ':hover': {
                                        color: 'var(--color-brandBgNavFont)',
                                    },
                                }

                            return {
                                'paddingLeft': '0',
                                'paddingRight': '0',
                                'paddingTop': '0',
                                'paddingBottom': '0',
                                'borderLeftWidth': '0',
                                'backgroundColor': 'none',
                                'backgroundImage': 'none',
                                'borderRadius': '8px',
                                'color': 'var(--color-brandBgNavFontGray)',
                                ':hover': {
                                    color: 'var(--color-brandBgNavFont)',
                                    backgroundColor: 'var(--color-brandPrimaryHover)',
                                },
                            }
                        },
                    },
                    NavLink: {
                        style: {},
                    },
                }}
                activeItemId={activeItemId ?? (baseuiNavItems[0]?.itemId as string)}
                items={baseuiNavItems}
                onChange={({ event, item }) => {
                    event.preventDefault()
                    history.push(item.itemId)
                }}
            />
            <div
                style={{
                    display: 'flex',
                    flexDirection: ctx.expanded ? 'row' : 'column',
                    alignItems: 'center',
                    height: 48,
                    position: 'relative',
                    borderTop: '1px solid var(--color-brandBgNavBorder)',
                }}
            >
                <div
                    style={{
                        flexGrow: 1,
                        width: ctx.expanded ? sidebarExpandedWidth - sidebarFoldedWidth : sidebarFoldedWidth,
                    }}
                />
                <div
                    role='button'
                    tabIndex={0}
                    onClick={handleExpandedClick}
                    style={{
                        position: 'absolute',
                        right: 0,
                        top: 0,
                        bottom: 0,
                        cursor: 'pointer',
                        display: 'flex',
                        flexDirection: 'row',
                        alignItems: 'center',
                        color: 'var(--color-brandBgNavFont)',
                    }}
                >
                    <div
                        style={{
                            display: 'inline-flex',
                            float: 'right',
                            alignSelf: 'center',
                            width: sidebarFoldedWidth,
                            justifyContent: 'center',
                        }}
                    >
                        {ctx.expanded ? <AiOutlineDoubleLeft /> : <AiOutlineDoubleRight />}
                    </div>
                </div>
            </div>
        </div>
    )
}
