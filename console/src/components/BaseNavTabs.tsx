import React from 'react'
import { Tabs, Tab, TabsProps, TabProps } from 'baseui/tabs-motion'
import { useHistory } from 'react-router-dom'
import { StatefulTooltip } from 'baseui/tooltip'
import { mergeOverrides } from '@/utils/baseui'
import { themedUseStyletron } from '@starwhale/ui/theme/styletron'
import { INavItem } from './BaseSidebar'
import { useRouterActivePath } from '@/hooks/useRouterActivePath'

export interface IComposedNavTabsProps {
    // style?: React.CSSProperties
    // navStyle?: React.CSSProperties
    fill?: TabsProps['fill']
    tabsOverrides?: TabsProps['overrides']
    tabOverrides?: TabProps['overrides']
}

export interface IBaseNavTabsProps extends IComposedNavTabsProps {
    navItems: INavItem[]
}

export function BaseNavTabs({ navItems, fill = 'intrinsic', tabsOverrides, tabOverrides }: IBaseNavTabsProps) {
    const history = useHistory()
    const [, theme] = themedUseStyletron()
    const { activeItemPath } = useRouterActivePath(navItems)
    if (!navItems || navItems.length === 0) {
        return null
    }

    return (
        <Tabs
            activeKey={activeItemPath}
            onChange={({ activeKey }) => {
                history.push(activeKey as string)
            }}
            fill={fill}
            activateOnFocus
            overrides={mergeOverrides(
                {
                    TabHighlight: {
                        style: {
                            backgroundColor: theme.brandPrimary,
                            height: '4px',
                            bottom: '4px',
                        },
                    },
                    TabBorder: {
                        style: {
                            height: '1px',
                        },
                    },
                },
                tabsOverrides
            )}
        >
            {navItems.map((item) => {
                const Icon = item.icon
                return (
                    <Tab
                        overrides={mergeOverrides(
                            {
                                TabPanel: {
                                    style: {
                                        paddingLeft: '0',
                                        paddingRight: '0',
                                        paddingBottom: '0',
                                        paddingTop: 0,
                                    },
                                },
                                Tab: {
                                    style: {
                                        'background': 'transparent',
                                        'color': item.path === activeItemPath ? theme.brandPrimary : '',
                                        ':hover': {
                                            background: 'transparent',
                                            color: theme.brandPrimary,
                                        },
                                    },
                                },
                            },
                            tabOverrides
                        )}
                        disabled={item.disabled}
                        key={item.path}
                        title={
                            <div
                                style={{
                                    display: 'flex',
                                    alignItems: 'center',
                                    gap: 12,
                                    lineHeight: '24px',
                                    height: 24,
                                    textOverflow: 'ellipsis',
                                    whiteSpace: 'nowrap',
                                    overflow: 'hidden',
                                }}
                            >
                                {Icon}
                                <div
                                    style={{
                                        display: 'flex',
                                        alignItems: 'center',
                                        gap: 6,
                                    }}
                                >
                                    <span>{item.title}</span>
                                    {item.helpMessage && (
                                        <StatefulTooltip content={item.helpMessage} showArrow>
                                            <div
                                                style={{
                                                    display: 'inline-flex',
                                                    cursor: 'pointer',
                                                }}
                                            />
                                        </StatefulTooltip>
                                    )}
                                </div>
                            </div>
                        }
                    />
                )
            })}
        </Tabs>
    )
}
