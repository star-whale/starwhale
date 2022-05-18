/* eslint-disable react/no-unused-prop-types */

import React, { useMemo } from 'react'
import { Tabs, Tab } from 'baseui/tabs-motion'
import { useHistory, useLocation } from 'react-router-dom'
import _ from 'lodash'
import { StatefulTooltip } from 'baseui/tooltip'
import { AiOutlineQuestionCircle } from 'react-icons/ai'
import { INavItem } from './BaseSidebar'

export interface IComposedNavTabsProps {
    style?: React.CSSProperties
    navStyle?: React.CSSProperties
}

export interface IBaseNavTabsProps extends IComposedNavTabsProps {
    navItems: INavItem[]
}

export function BaseNavTabs({ navItems }: IBaseNavTabsProps) {
    const history = useHistory()
    const location = useLocation()

    const activeItemId = useMemo(() => {
        const item = navItems
            .slice()
            .reverse()
            .find((item_) => _.startsWith(location.pathname, item_.path))
        return item?.path
    }, [location.pathname, navItems])

    return (
        <Tabs
            activeKey={activeItemId}
            onChange={({ activeKey }) => {
                history.push(activeKey as string)
            }}
            fill='intrinsic'
            activateOnFocus
            overrides={{
                TabHighlight: {
                    style: {
                        background: 'var(--color-brandPrimary)',
                    },
                },
            }}
        >
            {navItems.map((item) => {
                const Icon = item.icon
                return (
                    <Tab
                        overrides={{
                            TabPanel: {
                                style: {
                                    padding: '0px !important',
                                },
                            },
                            Tab: {
                                style: {
                                    'background': 'transparent',
                                    'color': item.path === activeItemId ? 'var(--color-brandPrimary)' : '',
                                    ':hover': {
                                        background: 'transparent',
                                        color: 'var(--color-brandPrimary)',
                                    },
                                },
                            },
                        }}
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
                                            >
                                                <AiOutlineQuestionCircle />
                                            </div>
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
