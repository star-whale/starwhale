/* eslint-disable react/no-unused-prop-types */

import React, { useMemo } from 'react'
import { Tabs, Tab, TabsProps, TabProps } from 'baseui/tabs-motion'
import { useHistory, useLocation } from 'react-router-dom'
import _ from 'lodash'
import { StatefulTooltip } from 'baseui/tooltip'
import { mergeOverrides } from '@/utils/baseui'
import { themedUseStyletron } from '@starwhale/ui/theme/styletron'
import { INavItem } from './BaseSidebar'

export interface IComposedNavTabsProps {
    style?: React.CSSProperties
    navStyle?: React.CSSProperties
    fill?: TabsProps['fill']
    tabsOverrides?: TabsProps['overrides']
    tabOverrides?: TabProps['overrides']
}

export interface IBaseNavTabsProps extends IComposedNavTabsProps {
    navItems: INavItem[]
}

export function BaseNavTabs({ navItems, fill = 'intrinsic', tabsOverrides, tabOverrides }: IBaseNavTabsProps) {
    const history = useHistory()
    const location = useLocation()
    const [, theme] = themedUseStyletron()

    const activeItemId = useMemo(() => {
        const item = navItems
            .slice()
            .reverse()
            .find((item_) => _.startsWith(location.pathname, item_.path) || _.startsWith(item_.path, location.pathname))
        return item?.path
    }, [location.pathname, navItems])

    return (
        <Tabs
            activeKey={activeItemId}
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
                                        'color': item.path === activeItemId ? theme.brandPrimary : '',
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
