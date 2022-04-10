import { Item, Navigation } from 'baseui/side-navigation'
import _ from 'lodash'
import React, { useCallback, useContext, useMemo } from 'react'
import { useLocation, useHistory } from 'react-router-dom'
import useSidebarWidth from '@/hooks/useSidebarWidth'
import { useStyletron } from 'baseui'
import type { IconBaseProps } from 'react-icons/lib'
import { SidebarContext } from '@/contexts/SidebarContext'
import { sidebarExpandedWidth, sidebarFoldedWidth } from '@/consts'
import { AiOutlineDoubleLeft, AiOutlineDoubleRight } from 'react-icons/ai'
import color from 'color'
import Text from '@/components/Text'
import useTranslation from '@/hooks/useTranslation'

export interface IComposedSidebarProps {
    style?: React.CSSProperties
    navStyle?: React.CSSProperties
}

export interface INavItem {
    title: string
    icon?: React.ComponentType<IconBaseProps>
    path?: string
    children?: INavItem[]
    disabled?: boolean
    helpMessage?: React.ReactNode
    activePathPattern?: RegExp
    isActive?: () => boolean
}

function transformNavItems(navItems: INavItem[], expanded = true): Item[] {
    return navItems.map((item) => {
        const { icon: Icon } = item
        return {
            title: (
                <div
                    style={{
                        display: 'flex',
                        alignItems: 'center',
                        gap: 12,
                        fontSize: 14,
                        lineHeight: '40px',
                        height: 40,
                        textOverflow: 'ellipsis',
                        whiteSpace: 'nowrap',
                        overflow: 'hidden',
                        paddingLeft: expanded ? 15 : 12,
                    }}
                >
                    {Icon && <Icon size={26} />}
                    {expanded && <span>{item.title}</span>}
                </div>
            ),
            itemId: item.path,
        }
    })
}

export interface IBaseSideBarProps extends IComposedSidebarProps {
    title?: string
    icon?: React.ComponentType<IconBaseProps>
    navItems: INavItem[]
}

export default function BaseSidebar({ navItems, style, title, icon }: IBaseSideBarProps) {
    const width = useSidebarWidth()
    const ctx = useContext(SidebarContext)

    const history = useHistory()
    const location = useLocation()

    const baseuiNavItems = useMemo(() => transformNavItems(navItems, ctx.expanded), [ctx.expanded, navItems])

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

    const [, theme] = useStyletron()

    const handleExpandedClick = useCallback(() => {
        if (ctx.expanded) {
            ctx.setExpanded(false)
        } else {
            ctx.setExpanded(true)
        }
    }, [ctx])

    const [t] = useTranslation()

    return (
        <div
            style={{
                width,
                display: 'flex',
                flexShrink: 0,
                flexDirection: 'column',
                flexBasis: width,
                overflow: 'hidden',
                overflowY: 'auto',
                background: theme.colors.backgroundPrimary,
                borderRight: `1px solid ${theme.borders.border200.borderColor}`,
                transition: 'all 200ms cubic-bezier(0.7, 0.1, 0.33, 1) 0ms',
                ...style,
            }}
        >
            {ctx.expanded && title && icon && (
                <div
                    style={{
                        display: 'flex',
                        gap: 14,
                        fontSize: '11px',
                        alignItems: 'center',
                        padding: '8px 8px 8px 15px',
                        borderBottom: `1px solid ${theme.borders.border200.borderColor}`,
                        overflow: 'hidden',
                    }}
                >
                    {React.createElement(icon, { size: 10 })}
                    <Text
                        style={{
                            fontSize: '12px',
                            overflow: 'hidden',
                            textOverflow: 'ellipsis',
                            whiteSpace: 'nowrap',
                        }}
                    >
                        {title}
                    </Text>
                </div>
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
                    NavItem: {
                        style: {
                            padding: '8px 8px 8px 8px',
                        },
                    },
                    NavLink: {
                        style: {
                            color: 'var(--color-brandIndicatorRegular)',
                        },
                    },
                }}
                activeItemId={activeItemId ?? (baseuiNavItems[0]?.itemId as string)}
                items={baseuiNavItems}
                onChange={({ event, item }) => {
                    event.preventDefault()
                    history.push(item.itemId)
                }}
            />
            <div>
                <div
                    style={{
                        display: 'flex',
                        flexDirection: ctx.expanded ? 'row' : 'column',
                        alignItems: 'center',
                        height: 40,
                        position: 'relative',
                        borderTop: `1px solid ${theme.borders.border100.borderColor}`,
                    }}
                >
                    <div
                        style={{
                            flexGrow: 1,
                            width: ctx.expanded ? sidebarExpandedWidth - sidebarFoldedWidth : sidebarFoldedWidth,
                        }}
                    ></div>
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
        </div>
    )
}
