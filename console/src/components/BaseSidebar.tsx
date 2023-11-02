import { Navigation } from 'baseui/side-navigation'
import _ from 'lodash'
import React, { useCallback, useContext, useMemo } from 'react'
import { useLocation, useHistory } from 'react-router-dom'
import useSidebarWidth from '@/hooks/useSidebarWidth'
import type { IconBaseProps } from 'react-icons/lib'
import { SidebarContext } from '@/contexts/SidebarContext'
import { createUseStyles } from 'react-jss'
import IconFont from '@starwhale/ui/IconFont'
import { StatefulTooltip } from 'baseui/tooltip'
import TextLink from './Link/TextLink'
import { themedUseStyletron } from '@starwhale/ui/theme/styletron'

const EXPANDED_PADDING = '27px'
const FOLDED_PADDING = '0px'
const useBaseSideBarStyles = createUseStyles({
    sidebarWrapper: {
        display: 'flex',
        flexShrink: 0,
        flexDirection: 'column',
        overflow: 'hidden',
        overflowY: 'auto',
        background: '#FFFFFF',
        transition: 'all 200ms cubic-bezier(0.7, 0.1, 0.33, 1) 0ms',
        color: 'rgba(2,16,43,0.60)',
        borderRight: '1px solid #E2E7F0',
    },
    siderTitle: {
        height: '48px',
        backgroundColor: '#F7F8FA',
        color: '#02102B',
        display: 'flex',
        gap: 8,
        fontSize: '14px',
        placeItems: 'center',
        padding: '8px 26px 8px 26px',
        overflow: 'hidden',
        textDecoration: 'none',
        marginBottom: '5px',
        paddingLeft: EXPANDED_PADDING,
        alignItems: 'center',
    },
    siderNavLink: {
        display: 'flex',
        alignItems: 'center',
        fontSize: 14,
        lineHeight: '40px',
        height: 38,
        gap: 10,
        textOverflow: 'ellipsis',
        whiteSpace: 'nowrap',
        overflow: 'hidden',
    },
})

export interface IComposedSidebarProps {
    style?: React.CSSProperties
}

export interface INavItem {
    title: string | React.ReactNode
    icon?: React.ComponentType<IconBaseProps> | React.ReactNode | any
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
    const [, theme] = themedUseStyletron()
    const styles = useBaseSideBarStyles({ theme })

    const history = useHistory()
    const location = useLocation()

    const baseuiNavItems = useMemo(() => {
        return navItems.map((item) => {
            const { icon: Icon } = item
            return {
                title: (
                    <div
                        className={styles.siderNavLink}
                        style={{
                            paddingLeft: ctx.expanded ? '18px' : '',
                            justifyContent: ctx.expanded ? 'flex-start' : 'center',
                        }}
                    >
                        {ctx.expanded && (Icon as any)}
                        {ctx.expanded && <span>{item.title}</span>}
                        {!ctx.expanded && (
                            <StatefulTooltip content={item.title} placement='bottomRight'>
                                <div>{Icon}</div>
                            </StatefulTooltip>
                        )}
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

    const $title = (
        <div className={styles.siderTitle}>
            <span className='flex-shrink-0 lh-none'>{icon}</span>
            {ctx.expanded && <span className='font-bold overflow-hidden text-ellipsis'>{title}</span>}
        </div>
    )

    return (
        <div
            className={styles.sidebarWrapper}
            style={{
                flexBasis: width,
                width,
                ...style,
            }}
        >
            {title && icon && (
                <div>
                    {titleLink ? (
                        <TextLink
                            to={titleLink}
                            tooltip={{
                                content: title,
                                placement: 'bottomRight',
                            }}
                        >
                            {$title}
                        </TextLink>
                    ) : (
                        $title
                    )}
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
                    NavItemContainer: {
                        style: {
                            height: '38px',
                            padding: '5px 8px',
                            boxSizing: 'border-box',
                            borderLeftWidth: '0',
                            backgroundImage: 'none',
                            backgroundColor: 'none',
                            marginBottom: '10px',
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
                                    'borderRadius': '8px',
                                    'color': '#2B65D9',
                                    'backgroundColor': '#F0F4FF',
                                    ':hover': {
                                        color: '#2B65D9',
                                        backgroundColor: '#F0F4FF',
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
                                'color': 'rgba(2,16,43,0.60)',
                                ':hover': {
                                    color: '#2B65D9',
                                    backgroundColor: '#F0F4FF',
                                },
                            }
                        },
                    },
                    NavLink: {
                        style: {},
                    },
                }}
                activeItemId={activeItemId ?? ''}
                items={baseuiNavItems}
                onChange={({ event, item }) => {
                    event.preventDefault()
                    if (item.itemId) history.push(item.itemId)
                }}
            />
            <div
                style={{
                    display: 'flex',
                    alignItems: 'center',
                    height: 48,
                    position: 'relative',
                    borderTop: '1px solid #EEF1F6',
                    paddingLeft: ctx.expanded ? EXPANDED_PADDING : FOLDED_PADDING,
                    justifyContent: ctx.expanded ? 'flex-start' : 'center',
                }}
            >
                <div
                    role='button'
                    tabIndex={0}
                    onClick={handleExpandedClick}
                    style={{
                        cursor: 'pointer',
                        display: 'flex',
                        flexDirection: 'row',
                        color: 'rgba(2,16,43,0.60)',
                        alignItems: 'center',
                    }}
                >
                    <IconFont type={ctx.expanded ? 'fold' : 'unfold'} />
                </div>
            </div>
        </div>
    )
}
